(ns yetibot.core.test.commands.config
  (:require
    [yetibot.core.config :as config]
    [yetibot.core.commands.config :refer :all]
    [clojure.test :refer :all]))

(def test-config {:yetibot
                  {:quod {:libet "light"}
                   :foo {:bar "baz"}}})

(defn wrap [f]
  ; temporarily set a custom config to test under
  (def old-config (deref #'config/config))
  (reset! @#'config/config test-config)
  (f)
  (reset! @#'config/config old-config))

(use-fixtures :once wrap)

(deftest test-config-lookup
  (is (= "\"light\"\n" (lookup-config {:match "yetibot quod libet"})))
  (is (= "\"baz\"\n"(lookup-config {:match "foo bar"}))))
