(ns yetibot.core.hooks
  (:require
    [taoensso.timbre :refer [info warn error]]
    [yetibot.core.util :refer [with-fresh-db]]
    [yetibot.core.handler]
    [clojure.string :as s]
    [yetibot.core.interpreter :refer [handle-cmd]]
    [yetibot.core.models.help :as help]
    [robert.hooke :as rh]
    [clojure.stacktrace :as st]
    [clojure.contrib.cond :refer [cond-let]]))

(def ^:private Pattern java.util.regex.Pattern)

(defn suppress
  "Wraps parameter in meta data to indicate that it should not be posted to campfire"
  [data-structure]
  (with-meta data-structure {:suppress true}))

(defn cmd-unhook [topic]
  (help/remove-docs topic)
  (rh/remove-hook #'handle-cmd topic))

(defmacro cmd-hook [prefix & exprs]
  ; let consumer pass [topic regex] as prefix arg when a (str regex) isn't enough
  (let [[topic prefix] (if (vector? prefix) prefix [(str prefix) prefix])
        callback (gensym "callback")
        cmd-with-args (gensym "cmd-with-args")
        cmd (gensym "cmd")
        args (gensym "args")
        user (gensym "user")
        opts (gensym "opts")
        match (gensym "match")
        chat-source (gensym "chat-source")
        extra (gensym "extra")]
    `(do
       (rh/add-hook
         #'handle-cmd
         ~topic ; use topic string as the hook-key to enable removing/re-adding
         ; (fn [~callback ~cmd ~args ~user ~opts])
         (fn [~callback ~cmd-with-args {~chat-source :chat-source
                                        ~user :user
                                        ~opts :opts
                                        :as ~extra}]
           (let [[~cmd & ~args] (s/split ~cmd-with-args #"\s+")
                 ~args (s/join " " ~args)]
             ; only match against the first word in ~args
             (if (re-find ~prefix (s/lower-case ~cmd))
               (do
                 (info "found" ~prefix "on cmd" ~cmd
                       ; "opts:" ~opts ; "extra" ~extra
                       "args:" ~args)
                 ; try matching the available sub-commands
                 (cond-let [~match]
                           ; rebuild the pairs in `exprs` as valid input for cond-let
                           ~@(map (fn [i#]
                                    (cond
                                      ; prefix to match
                                      (instance? Pattern i#) `(re-find ~i# ~args)
                                      ; placeholder / fallthrough - set match equal to
                                      ; :empty, which will trigger this match for
                                      ; cond-let while not explicitly matching
                                      ; anything.
                                      (= i# '_) `(or :empty)
                                      ; send result back to hooked fn
                                      :else `(~i# {:cmd ~cmd
                                                   :args ~args
                                                   :match ~match
                                                   :user ~user
                                                   :chat-source ~chat-source
                                                   :opts ~opts})))
                                  exprs)
                           ; default to help
                           true (yetibot.core.handler/handle-unparsed-expr (str "help " ~topic))))
               (~callback ~cmd-with-args ~extra)))))
       ; extract the meta from the commands and use it to build docs
       (help/add-docs ~topic
                      (map
                        (fn [i#]
                          (when (and (symbol? i#) (not= i# '_))
                            (:doc (meta (resolve i#)))))
                        '~exprs)))))

(defn obs-hook
  "Pass a collection of event-types you're interested in and an observer function
   that accepts a single arg. If an event occurs that matches the events in your
   event-types arg, your observer will be called with the event's json."
  [event-types observer]
  (rh/add-hook
    #'yetibot.core.handler/handle-raw
    (let [event-types (set event-types)]
      (fn [callback chat-source user event-type body]
        (when (contains? event-types event-type)
          (with-fresh-db
            (observer {:chat-source chat-source
                       :event-type event-type
                       :user user
                       :body body})))
        (callback chat-source user event-type body)))))
