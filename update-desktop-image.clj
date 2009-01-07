(ns update-desktop-image
  (:import (java.net URL)
           (java.io FileOutputStream))
  (:require [clojure.contrib.duck-streams :as streams]
            [clojure.contrib.shell-out :as sout]))

(def ftp-str (str "ftp://anonymous: @ftp.gnome.org" "/Public/GNOME/teams/art.gnome.org/backgrounds/"))

(def image-name (str (.getProperty (System/getProperties) "user.home") "/desktop-image.jpg"))

(defn potential-files [url resolution]
    (for [name (map #(last (.split %1 " ")) 
                 (.split (streams/slurp* url) "\n")) 
        :when (.endsWith name (str "_" resolution ".jpg"))] name))

(defn write-bytes 
  "Writes the bytes from the in-stream to the given filename."
  [#^java.io.InputStream in-stream #^String filename]
  (with-open [out-stream (new FileOutputStream filename)]
    (let [buffer (make-array (Byte/TYPE) 4096)]
      (loop [bytes (.read in-stream buffer)]
        (if (not (neg? bytes))
          (do 
            (.write out-stream buffer 0 bytes)
            (recur (.read in-stream buffer))))))))

(defn select-file [ftp-str resolution]
  (let [files (potential-files (URL. ftp-str) resolution)]
    (str ftp-str (nth files (rand-int (count files))))))

(defn update [resolution]
   (let [tmp-image-name (str image-name ".tmp")]
     (with-open [r (.openStream (URL. (select-file ftp-str resolution)))]
       (write-bytes r tmp-image-name))
     (sout/sh "mv" "-f" tmp-image-name image-name)
     (sout/sh "/usr/bin/gconftool" "-s" 
              "/desktop/gnome/background/picture_filename -t string \"" image-name "\"")))

(update (or (first *command-line-args*) "1680x1050"))

(comment " 
  Call this script from the command line and only takes one argument, your screen resolution.
  This assumes you are running gnome and have access to the internet.

  clj update-desktop-image 1024x178
  clj update-desktop-image ; this assumes 1680x1050 resolution")



