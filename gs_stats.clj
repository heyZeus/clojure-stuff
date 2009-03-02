(ns gs-stats
  (:import (org.apache.commons.httpclient HttpClient NameValuePair URI)
           (org.apache.commons.httpclient.cookie CookiePolicy CookieSpec)
           (org.apache.commons.httpclient.methods GetMethod PostMethod DeleteMethod 
                                                  TraceMethod HeadMethod PutMethod)))

(defmacro send-uri
  [client uri & body]
  `(try 
     (.executeMethod ~client ~uri)
     ~@body
     (finally (.releaseConnection ~uri))))

(defn client 
  [server]
  (let [c (HttpClient.)]
    (.. c (getHostConfiguration) (setHost (URI. server true)))
    c))

(defn add-param
  [method k & vs]
  (doseq [v vs] (.addParameter method (name k) (str v))))

(defn uri
  ([path method url-params]
   (let [key-method (if method (keyword method) nil)
         m (cond 
             (= :post key-method) (PostMethod. path)
             (= :delete key-method) (DeleteMethod. path)
             (= :put key-method) (PutMethod. path)
             (= :trace key-method) (TraceMethod. path)
             (= :head key-method) (HeadMethod. path)
             :else (GetMethod. path))]
         (doseq [[k v] url-params]
               (add-param m k v))
         m))
  ([path method] (uri path method nil)) 
  ([path] (uri path nil nil))) 

(defn cookies
  [client]
  (.. client (getState) (getCookies)))

(defn print-cookies
  [client]
  (doseq [c (cookies client)] (println c)))

(defn res-str
  [uri]
  (.getResponseBodyAsString uri))

(defn assert-cookie-names
  [client & cookie-names]
  (let [actual-cookies (cookies client)]
    (every? (fn [exp-cookie-name] 
              (some #(= exp-cookie-name (.getName %1)) actual-cookies))
            cookie-names)))

(let [client (client "http://www.guidespot.com")
      login (uri "/")] 
  (send-uri client login   
    (println (res-str login))))
