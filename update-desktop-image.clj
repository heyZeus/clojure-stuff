(ns update-desktop-image
  (:import (java.net URL)
           (java.io FileOutputStream))
  (:require [clojure.contrib.duck-streams :as streams]
            [clojure.contrib.shell-out :as sout]))

(def ftp-str (str "ftp://anonymous: @ftp.gnome.org" "/Public/GNOME/teams/art.gnome.org/backgrounds/"))

(defn potential-files [url resolution]
    (for [name (map #(last (.split %1 " ")) 
                 (.split (streams/slurp* url) "\n")) 
        :when (.endsWith name (str "_" resolution ".jpg"))] name))

(defn write-bytes-to-file [instream filename]
  (with-open [ostream (new FileOutputStream filename)]
    (let [buffer (make-array (Byte/TYPE) 4096)]
      (loop [bytes (.read instream buffer)]
        (if (not (neg? bytes))
          (do 
            (.write ostream buffer 0 bytes)
            (recur (.read instream buffer))))))))

(defn select-file [ftp-str resolution]
  (let [files (potential-files (URL. ftp-str) resolution)]
    (str ftp-str (nth files (rand-int (count files))))))

(defn update [resolution download-file]
   (let [tmp-download-file (str download-file ".tmp")]
     (with-open [r (.openStream (URL. (select-file ftp-str resolution)))]
       (write-bytes-to-file r tmp-download-file))
     (sout/sh "mv" "-f" tmp-download-file download-file)
     (sout/sh "/usr/bin/gconftool" "-s" 
              "/desktop/gnome/background/picture_filename -t string \"" download-file "\"")))

(defn cmd-line-args [& defaults]
  (concat () *command-line-args* defaults))

(let [[resolution file] (cmd-line-args "1680x1050"
                                       (str (.getProperty (System/getProperties) "user.home") "/desktop-image.jpg"))]
  (update resolution file))

