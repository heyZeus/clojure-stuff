(ns update-desktop-image
  (:import (java.net URL)
           (java.io FileOutputStream)
           (java.io File))
  (:require [clojure.contrib.duck-streams :as streams]
            [clojure.contrib.shell-out :as sout]))

(def ftp-str (str "ftp://anonymous: @ftp.gnome.org" "/Public/GNOME/teams/art.gnome.org/backgrounds/"))


(defn potential-images
  "Returns a seq of potential image filenames, all relative paths."
  [url resolution]
    ; the output from the streams/slurp will be something like: 
    ; -rw-r--r--    1 ftp      ftp        480611 Aug 27  2006 OTHER-WhiteQueenKilledTheKing_1024x768.jpg
    ; -rw-r--r--    1 ftp      ftp        765493 Aug 27  2006 OTHER-WhiteQueenKilledTheKing_1280x1024.jpg
    ; -rw-r--r--    1 ftp      ftp        766046 Aug 27  2006 OTHER-WhiteQueenKilledTheKing_1400x1050.jpg
    ; -rw-r--r--    1 ftp      ftp       1265120 Aug 27  2006 OTHER-WhiteQueenKilledTheKing_1920x1200.jpg
    ; need to split on the space and grab the last field to get the filename, then check to see if 
    ; the filename has the correct screen resolution in the name
    (for [name (map #(last (.split %1 " ")) (.split (streams/slurp* url) "\n")) 
            :when (.endsWith name (str "_" resolution ".jpg"))] name))

(defn select-new-image
  "Returns a random desktop filename to download, this is the full path to the file."
  [resolution]
  ; get the potential files and return the full file path
  (let [files (potential-images (URL. ftp-str) resolution)]
    (str ftp-str (nth files (rand-int (count files))))))

(defn update-image 
  "Sets the desktop background to an image downloaded randomly from the internet."
  [image-name resolution]
   (let [tmp-image-name (str image-name ".tmp")]
     ; downloading to a tmp file first because overwriting the existing
     ; file causes some problems with gnome
     (with-open [i (.openStream (URL. (select-new-image resolution)))]
       (binding [streams/*buffer-size* 4096]
         (streams/copy i (File. tmp-image-name))))
     (sout/sh "mv" "-f" tmp-image-name image-name)
     ; updates the background image
     (sout/sh "/usr/bin/gconftool" "-s" 
              "/desktop/gnome/background/picture_filename -t string \"" image-name "\"")))

(update-image 
  (str (.getProperty (System/getProperties) "user.home") "/desktop-image.jpg")
  (or (first *command-line-args*) "1680x1050"))

(comment " 
  Call this script from the command line and it only takes one argument, your screen resolution.
  This assumes you are running gnome and have access to the internet.

  clj update-desktop-image 1024x178
  clj update-desktop-image ; this assumes 1680x1050 resolution")
