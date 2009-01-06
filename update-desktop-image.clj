(ns update-desktop-image
  (:import (java.net URL)
           (java.io FileOutputStream))
  (:require [clojure.contrib.duck-streams :as streams]
            [clojure.contrib.shell-out :as sout]))

; this could be passed into the script
(def resolution "1680x1050")

(def output-file (str (.getProperty (System/getProperties) "user.home") "/desktop-image.jpg"))

(defn read-bytes-to-file [istream filename]
  (with-open [ostream (new FileOutputStream filename)]
    (let [buffer (make-array (Byte/TYPE) 4096)]
      (loop [b-read (.read istream buffer)]
        (if (not (neg? b-read))
          (do (.write ostream buffer 0 b-read)
            (recur (.read istream buffer)))))))) 

(let [base-dir "/Public/GNOME/teams/art.gnome.org/backgrounds/"
      ftp-url (str "ftp://anonymous: @ftp.gnome.org" base-dir)
      potential-files (for [name (map #(last (.split %1 " ")) 
                                      (.split (streams/slurp* (URL. ftp-url)) "\n")) 
                           :when (.endsWith name (str "_" resolution ".jpg"))] name)
      selected-file (nth potential-files (rand-int (count potential-files)))]
  (with-open [r (.openStream (URL. (str ftp-url selected-file)))]
    (read-bytes-to-file r output-file))
  (sout/sh (str "gconftool -s /desktop/gnome/background/picture_filename -t string \"" output-file "\"")))

