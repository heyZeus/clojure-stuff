(ns update-desktop-image
  (:import (java.net URL)
           (java.io FileOutputStream))
  (:require [clojure.contrib.duck-streams :as streams]
            [clojure.contrib.shell-out :as sout]))

(def ftp-str (str "ftp://anonymous: @ftp.gnome.org" "/Public/GNOME/teams/art.gnome.org/backgrounds/"))

(defn potential-files 
  "Returns a seq of potential image filenames, all relative paths."
  [url resolution]
    (for [name (map #(last (.split %1 " ")) (.split (streams/slurp* url) "\n")) 
            :when (.endsWith name (str "_" resolution ".jpg"))] name))

(defn copy-stream
  "Writes the data from the istream to the ostream."
  ([#^java.io.InputStream istream #^java.io.OutputStream ostream buffer-size]
    (let [buffer (make-array (Byte/TYPE) buffer-size)]
      (loop [bytes (.read istream buffer)]
        (when (pos? bytes)
          (.write ostream buffer 0 bytes)
          (recur (.read istream buffer))))))
  ([#^java.io.InputStream istream #^java.io.OutputStream ostream]
    (copy-stream istream ostream 4096)))

(defn write-stream-to-file
  "Writes the data from the istream to the file."
  ([#^java.io.InputStream istream #^String filename buffer-size]
    (with-open [ostream (new FileOutputStream filename)]
      (copy-stream istream ostream buffer-size)))
  ([#^java.io.InputStream istream #^String filename]
    (write-stream-to-file istream filename 4096)))

(defn select-file 
  "Returns a random desktop image filename to download"
  [resolution]
  (let [files (potential-files (URL. ftp-str) resolution)]
    (str ftp-str (nth files (rand-int (count files))))))

(defn update 
  "Sets the desktop background to an image downloaded randomly from the internet."
  [image-name resolution]
   (let [tmp-image-name (str image-name ".tmp")]
     ; downloading to a tmp file first because overwriting the existing
     ; file causes some problems with gnome
     (with-open [r (.openStream (URL. (select-file resolution)))]
       (write-stream-to-file r tmp-image-name))
     (sout/sh "mv" "-f" tmp-image-name image-name)
     (sout/sh "/usr/bin/gconftool" "-s" 
              "/desktop/gnome/background/picture_filename -t string \"" image-name "\"")))

(update (str (.getProperty (System/getProperties) "user.home") "/desktop-image.jpg")
        (or (first *command-line-args*) "1680x1050"))

(comment " 
  Call this script from the command line and only takes one argument, your screen resolution.
  This assumes you are running gnome and have access to the internet.

  clj update-desktop-image 1024x178
  clj update-desktop-image ; this assumes 1680x1050 resolution")



