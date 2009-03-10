(ns gs-stats
  (:require [clj-web-crawler :as wc])
  (:import (java.util Calendar)))

(defn login 
  [login pass]
  (let [site (wc/client "http://www.guidespot.com")
        login (wc/method "/accounts/login" :post {:login login :password pass})]
    (wc/crawl site login
      (if (wc/assert-cookie-names site "_localguides_session")
        site))))

(defn gs-stats-today
  [client]
  (let [today (Calendar/getInstance)
        month (+ 1 (.get today (Calendar/MONTH)))
        year (.get today (Calendar/YEAR))
        day (.get today (Calendar/DAY_OF_MONTH))
        today-form (wc/method "admin" :get {"date_from[month]" month
                                            "date_from[year]" year
                                            "date_from[day]" day 
                                            "date_to[month]" month
                                            "date_to[year]" year
                                            "date_to[day]" day})
        result (wc/crawl-response client today-form)]
        (println result)))

(let [site (login (first *command-line-args*) (second *command-line-args*))]
  (gs-stats-today site))



      
      
        
  

