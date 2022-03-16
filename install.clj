#!/usr/local/bin/bb
(require '[babashka.process :refer [process]])

(declare run-which-cmd)
(def bash-path (delay (run-which-cmd "bash")))
(def brew-path (delay (run-which-cmd "brew")))
(def pip-path  (delay (run-which-cmd "pip")))
(def mas-path  (delay (run-which-cmd "mas")))

(defn run-which-cmd [cmd-lookup]
  (let [resp (shell/sh "which" cmd-lookup)]
    (when (pos? (:exit resp))
      (let [err (or (:err resp) (:out resp))]
        (throw (ex-info "Failed to find cmd path" {:cmd cmd-lookup :err err}))))
    (str/trim (:out resp))))

(defn read-csv [file]
  (with-open [reader (io/reader (io/file file))]
    (doall (csv/read-csv reader))))

(defn throw-err-if-bad [res]
  (when (> (:exit res) 0)
    (throw (ex-info "Error during install" {:msg (:err res)}))))

(defn run-proc [args]
  (-> @(process args {:out :inherit :err :string})
      (throw-err-if-bad)))


(defn bash [path]
  (run-proc [@bash-path "-c" path]))

(defn brew [name & {:keys [cask?]}]
  (let [args (->>
               [@brew-path "install" (when cask? "--cask") name]
               (filter some?))]
    (run-proc args)))

(defn mas [name]
  (run-proc [@mas-path "install" name]))

(defn pip [name]
  (run-proc [@pip-path "install" name]))

(defn app [name]
  (run-proc ["tar" "xzf" (str "./app/" name ".tar.gz")])
  (run-proc ["mv" (str name ".app") "/Applications/"]))

(defn try-install-entry [[type name path desc]]
  (println (format "Installing %s %s... " type name))
  (let [with-success (fn [f] (f) (println name "SUCCESS!"))]
    (try
      (cond
        (str/starts-with? type "#")
        (println name "SKIPPED")

        (= "B" type)
        (with-success #(brew name))

        (= "BC" type)
        (with-success #(brew name :cask? true))

        (= "M" type)
        (with-success #(mas name))

        (= "S" type)
        (with-success #(bash path))

        (= "PIP" type)
        (with-success #(pip name))

        (= "A" type)
        (with-success #(app name)))
      (catch Exception e
        (println (format "%s ERROR \n%s" name (ex-data e))))))
  (println))

(defn install-sequentially [rows]
  (run! try-install-entry rows))

(defn install-batch [batch]
  ; TODO
  (->> (partition-all 5 batch)
       (run!
         (fn [rows]
           (let [composite-name
                 (->> rows
                      (map second)
                      (str/join " "))
                 type
                 (ffirst rows)]
             (try-install-entry [type composite-name]))))))

(defn install [file-path]
  (->> (read-csv file-path)
       ; drop header
       (drop 1)
       (install-sequentially)))

(defn main [args]
  (let [csv-file-path (first args)]
    (println csv-file-path)
    (assert (some? csv-file-path) "csv file not specified")
    (dorun (install csv-file-path))
    nil))

(main *command-line-args*)
