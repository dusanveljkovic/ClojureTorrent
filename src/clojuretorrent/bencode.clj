(ns clojuretorrent.bencode
  [:require clojure.string])

(def d (int \d))
(def i (int \i))
(def e (int \e))
(def l (int \l))
(def colon (int \:))

(declare read-bstring)

(defn read-number
  [input delim]
  (let [[parsing-input consumed-input] (split-with #(not= % delim) input)
        data (->> parsing-input
                  (map char)
                  (apply str)
                  (Long/parseLong))]
    {:data data, :input (rest consumed-input)}))

(defn string-or-binary
  [input]
  (if (every? #(<= 0 % 127) input)
    (apply str (map char input))
    (byte-array input)))

(defn read-string-data
  [input length]
  (let [[parsing-input consumed-input] (split-at length input)
        data (->> parsing-input
                  (string-or-binary))]
    {:data data, :input consumed-input}))

(defn skip-n
  [input n]
  {:data nil, :input (drop n input)})

(defn read-bytestring
  [input]
  (let [number-r (read-number input colon)
        string-r (read-string-data (number-r :input) (number-r :data))]
    string-r))

(defn read-list
  [input]
  (loop [consumed-input input
         acc '()]
    (let [read (read-bstring consumed-input)
          data (read :data)
          next-input (read :input)]
      (if (nil? data)
        {:data acc, :input next-input}
        (recur next-input (conj acc data))))))

(defn read-dict
  [input]
  (loop [consumed-input input
         acc {}]
    (let [read-key (read-bstring consumed-input)
          read-val (read-bstring (read-key :input))
          key (read-key :data)
          val (read-val :data)]
      (if (nil? key)
        {:data acc, :input (read-key :input)}
        (recur (read-val :input) (conj acc {key val}))))))

(defn read-bstring
  [input]
  (let [first-byte (first input)
        skipped (skip-n input 1)]
    (cond (= first-byte i) (read-number (skipped :input) e)
          (= first-byte l) (read-list (skipped :input))
          (= first-byte d) (read-dict (skipped :input))
          (= first-byte e) skipped
          (nil? first-byte) skipped
          :else (read-bytestring input))))

(defmulti write-bstring class)

(defmethod write-bstring String [x]
  (str (count x) ":" x))

(defmethod write-bstring Number [x]
  (str "i" x "e"))

(defmethod write-bstring clojure.lang.PersistentList [x]
  (let [list-encoded (clojure.string/join (map write-bstring x))]
   (str "l" list-encoded "e")))

(defmethod write-bstring clojure.lang.PersistentArrayMap [x]
  (let [sorted-x (into (sorted-map) x)
        map-encoded (clojure.string/join (map (fn [[key val]] (str (write-bstring key) (write-bstring val))) sorted-x))]
    (str "d" map-encoded "e")))

(defmethod write-bstring (Class/forName "[B") [x]
  (str (count x) ":" (String. x "ISO-8859-1")))

