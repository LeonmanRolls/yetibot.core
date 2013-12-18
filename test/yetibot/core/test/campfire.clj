(ns yetibot.core.test.campfire
  (:require
    [yetibot.core.chat :refer [contains-image-url-lines?]]
    [yetibot.core.util.format :refer :all]
    [clojure.test :refer :all]))

(def nested-list
  [["meme generator"
    "http://assets.diylol.com/hfs/de6/c7c/061/resized/mi-bok-meme-generator-i-look-both-ways-before-crossing-the-street-at-the-same-time-0dd824.jpg"]
   ["meme maker"
    "http://cdn9.staztic.com/app/a/128/128867/meme-maker-27-0-s-307x512.jpg"]
   ["meme creator"
    "http://img-ipad.lisisoft.com/img/2/9/2974-1-meme-creator-pro-caption-memes.jpg"]])

(def formatted-list (format-data-structure nested-list))

(deftest contains-image-url
  (is
    (contains-image-url-lines? (first formatted-list))
    "it should find image urls in the string"))
