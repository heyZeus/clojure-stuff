(ns gs-stats
  (:require [clj-web-crawler :as wc]
            [clojure.contrib.lazy-xml :as xml]
            [clojure.contrib.zip-filter :as zf])
  (:import (java.util Calendar)))

(defn login 
  [login pass]
  (let [site (wc/client "http://www.guidespot.com")
        login (wc/method "/accounts/login" :post {:login login :password pass})
        admin (wc/method "/admin")]
    (wc/crawl site login)
    (wc/crawl site admin
      (if (= 404 (.getStatusCode admin))
        (throw (Exception. "Username/password invalid"))
        site))))

(defn gs-stats-today
  [client]
  (let [today (Calendar/getInstance)
        month (+ 1 (.get today (Calendar/MONTH)))
        year (.get today (Calendar/YEAR))
        day (.get today (Calendar/DAY_OF_MONTH))
        today-form (wc/method "admin" :post {"date_from[month]" month
                                            "date_from[year]" year
                                            "date_from[day]" day 
                                            "date_to[month]" month
                                            "date_to[year]" year
                                            "date_to[day]" day})]
     (wc/crawl client today-form
        (-> (wc/response-str today-form) java.io.StringReader. xml/parse-trim xml/emit))))

(let [site (login (first *command-line-args*) (second *command-line-args*))]
  (gs-stats-today site))




      
      
        
  

