(ns minimily.utils.database
  (:require [hikari-cp.core    :refer :all]
            [clojure.string    :as str]
            [clojure.java.jdbc :as jdbc]
            [config.core       :refer [env]]))

(defn decompose-url [url]
  (let [double-slashes (str/index-of url "//")
        second-colon   (str/index-of url ":" (+ double-slashes 2))
        at             (str/index-of url "@")
        third-colon    (str/index-of url ":" at)
        slash          (str/index-of url "/" third-colon)]
    {:username      (subs url (+ double-slashes 2) second-colon)
     :password      (subs url (+ second-colon 1) at)
     :server-name   (subs url (+ at 1) third-colon)
     :port-number   (subs url (+ third-colon 1) slash)
     :database-name (subs url (+ slash 1))}))

(def options  (conj {:pool-name "db-pool" :adapter "postgresql"}
                    (decompose-url (:DATABASE_URL env))))

(def datasource
  (make-datasource options))

(defmacro with-conn [& body]
  `(jdbc/with-db-connection [~'conn {:datasource datasource}]
    ~@body))

(defn find-records [query]
  (with-conn
    (jdbc/query conn query)))

(defn get-record [table id]
  (find-records ["select * from ? where id = ?" table id]))

(defn insert-record
  "Returns the number of inserted records."
  [table record]
   (with-conn
    (let [record (dissoc record :id)]
      (jdbc/insert! conn table (keys record) (vals record)))))

(defn update-record [table record]
  (with-conn
    (let [id     (:id record)
          record (dissoc record :id)]
      (jdbc/update! conn table record ["id = ?" id]))))

(defn save-record [table record]
    (if (nil? (get record :id))
      (insert-record table record)
      (update-record table record)))

(defn delete-record
  "Returns the number of deleted records."
  [table id]
  (with-conn
    (jdbc/delete! conn table ["id = ?" id])))
