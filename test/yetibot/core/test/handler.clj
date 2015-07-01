(ns yetibot.core.test.handler
  (:require
    [yetibot.core.parser :refer [parser]]
    [yetibot.core.handler :refer :all]
    [yetibot.core.commands.history]
    [yetibot.core.repl :refer [load-minimal]]
    [instaparse.core :as insta]
    [clojure.test :refer :all]))

(load-minimal)

;; generate some history

(dotimes [i 10]
  (handle-raw
    {:adapter :test :room "foo"}
    {:id "yetitest"}
    :message
    (str "test history: " i)))

;; embedded commands

(deftest test-embedded-cmds
  (is
    (=
     ;; temp shouldn't be included because it's not a command/alias in the test
     ;; env
     (embedded-cmds "`echo your temp:` wonder what the `temp 98101` is")
     [[:expr [:cmd [:words "echo" [:space " "] "your" [:space " "] "temp:"]]]])
    "embedded-cmds should extract a collection of embedded commands from a string"))


