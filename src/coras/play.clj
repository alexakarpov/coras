;; This walkthrough introduces the core concepts of core.async.

;; The clojure.core.async namespace contains the public API.
(require '[clojure.core.async :as a :refer [chan
                                            close!
                                            >!!
                                            <!!
                                            thread
                                            >!
                                            <!
                                            go
                                            alt!
                                            alts!
                                            alts!!
                                            timeout
                                            dropping-buffer
                                            sliding-buffer
                                            go-loop
                                            ]])

;;;; CHANNELS


;; Data is transmitted on queue-like channels. By default channels
;; are unbuffered (0-length) - they require producer and consumer to
;; rendezvous for the transfer of a value through the channel.

;; Use `chan` to make an unbuffered channel:
(chan)

;; Pass a number to create a channel with a fixed buffer:
(chan 10)

;; `close!` a channel to stop accepting puts. Remaining values are still
;; available to take. Drained channels return nil on take. Nils may
;; not be sent over a channel explicitly!



(let [c (chan)]
  (close! c))

;;;; ORDINARY THREADS

;; In ordinary threads, we use `>!!` (blocking put) and `<!!`
;; (blocking take) to communicate via channels.

(let [c (chan 10)]
  (>!! c "hello")
  (assert (= "hello" (<!! c)))
  (close! c))

;; Because these are blocking calls, if we try to put on an
;; unbuffered channel, we will block the main thread. We can use
;; `thread` (like `future`) to execute a body in a pool thread and
;; return a channel with the result. Here we launch a background task
;; to put "hello" on a channel, then read that value in the current thread.

(let [c (chan)]
  (thread (>!! c "hello"))
  (assert (= "hello" (<!! c)))
  (close! c))

;;;; GO BLOCKS AND IOC THREADS

;; The `go` macro asynchronously executes its body in a special pool
;; of threads. Channel operations that would block will pause
;; execution instead, blocking no threads. This mechanism encapsulates
;; the inversion of control that is external in event/callback
;; systems. Inside `go` blocks, we use `>!` (put) and `<!` (take).

;; Here we convert our prior channel example to use go blocks:
(let [c (chan)]
  (go (>! c "hello"))
  (assert (= "hello" (<!! (go (<! c)))))
  (close! c))

;; Instead of the explicit thread and blocking call, we use a go block
;; for the producer. The consumer uses a go block to take, then
;; returns a result channel, from which we do a blocking take.

;;;; ALTS

;; One killer feature for channels over queues is the ability to wait
;; on many channels at the same time (like a socket select). This is
;; done with `alts!!` (ordinary threads) or `alts!` in go blocks.

;; We can create a background thread with alts that combines inputs on
;; either of two channels. `alts!!` takes a set of operations
;; to perform - either a channel to take from or a [channel value] to put
;; and returns the value (nil for put) and channel that succeeded:

(let [c1 (chan)
      c2 (chan)]
  (thread (while true
            (let [[v ch] (alts!! [c1 c2])]
              (println "Read" v "from" ch))))
  (>!! c1 "hi")
  (>!! c2 "there")
  (>!! c1 "more"))

;; Prints (on stdout, possibly not visible at your repl):
;;   Read hi from #<ManyToManyChannel ...>
;;   Read there from #<ManyToManyChannel ...>

;; We can use alts! to do the same thing with go blocks:

(let [c1 (chan)
      c2 (chan)]
  (go (while true
        (let [[v ch] (alts! [c1 c2])]
          (println "Read" v "from" ch))))
  (go (>! c1 "hi"))
  (go (>! c2 "there")))

;; Since go blocks are lightweight processes not bound to threads, we
;; can have LOTS of them! Here we create 1000 go blocks that say hi on
;; 1000 channels. We use alts!! to read them as they're ready.

(let [n 1000
      cs (repeatedly n chan)
      begin (System/currentTimeMillis)]
  (doseq [c cs] (go (>! c "hi")))
  (dotimes [i n]
    (let [[v c] (alts!! cs)]
      ;;(assert (= "hi" v))))
      (println (str "Read " v ))))
  (println "Read" n "msgs in" (- (System/currentTimeMillis) begin) "ms"))

;; `timeout` creates a channel that waits for a specified ms, then closes:

(let [t (timeout 100)
      begin (System/currentTimeMillis)]
  (<!! t)
  (println "Waited" (- (System/currentTimeMillis) begin)))

;; We can combine timeout with `alts!` to do timed channel waits.
;; Here we wait for 100 ms for a value to arrive on the channel, then
;; give up:

(let [c (chan)
      begin (System/currentTimeMillis)]
  (alts!! [c (timeout 3000)])
  (println "Gave up after" (- (System/currentTimeMillis) begin)))

;; ALT

;; todo

;;;; OTHER BUFFERS

;; Channels can also use custom buffers that have different policies
;; for the "full" case.  Two useful examples are provided in the API.

;; Use `dropping-buffer` to drop newest values when the buffer is full:
(chan (dropping-buffer 10))

;; Use `sliding-buffer` to drop oldest values when the buffer is full:
(chan (sliding-buffer 10))

;; (defn hot-dog-machine-v2
;;   [hotdog-count]
;;   (let [in (chan)
;;         out (chan)]
;;     (go (loop [hc hotdog-count]
;;           (if (> hc 0)
;;             (let [input (<! in)]
;;               (if (= 3 input)
;;                 (do (>! out "hot dog")
;;                     (recur (dec hc)))
;;                 (do (>! out "lettuce")
;;                     (recur hc))))
;;             (do (close! in)
;;                 (close! out)))))
;;     [in out]))

;; (let [[in out] (hot-dog-machine-v2 2)]
;;   (>!! in "pocket lint")
;;   (println (<!! out))

;;   (>!! in 3)
;;   (println (<!! out))

;;   (>!! in 3)
;;   (println (<!! out))

;;   (>!! in 3)
;;   (println (<!! out)))


;; ;;; ooooh this is cool:
;; (let [c1 (chan)
;;       c2 (chan)
;;       c3 (chan)]
;;   (go (>! c2 (clojure.string/upper-case (<! c1))))
;;   (go (>! c3 (clojure.string/reverse (<! c2))))
;;   (go (println (<! c3)))
;;   (>!! c1 "redrum"))

;; (defn upload
;;   [headshot c]
;;   (go (Thread/sleep (rand 100))
;;       (>! c headshot)))

;; (let [c1 (chan)
;;       c2 (chan)
;;       c3 (chan)]
;;   (upload "serious.jpg" c1)
;;   (upload "fun.jpg" c2)
;;   (upload "sassy.jpg" c3)
;;   (let [[headshot channel] (alts!! [c1 c2 c3])]
;;     (println "Sending headshot notification for" headshot)))


;; (defn run-timer [n]
;;   (a/go-loop [ticks 1]
;;     (do
;;       (println "tick..")
;;       (Thread/sleep 1000)
;;       (if (= ticks n) (println "bye!") (recur (+ 1 ticks))))))

;; (let [c1 (chan)
;;       c2 (chan)]
;;   (go (<! c2))
;;   (let [[value channel] (alts!! [c1 [c2 "put!"]])]
;;     (println value)
;;     (= channel c2)))


;; ;; RANDOM QUOTES

;; (defn append-to-file
;;   "Write a string to the end of a file"
;;   [filename s]
;;   (spit filename s :append true))

;; (defn format-quote
;;   "Delineate the beginning and end of a quote because it's convenient"
;;   [quote]
;;   (str "=== BEGIN QUOTE ===\n" quote "=== END QUOTE ===\n\n"))

;; (defn random-quote
;;   "Retrieve a random quote and format it"
;;   []
;;   (format-quote (slurp "http://www.braveclojure.com/random-quote")))

;; (defn snag-quotes
;;   [filename num-quotes]
;;   (let [c (chan)]
;;     (go (while true (append-to-file filename (<! c))))
;;     (dotimes [n num-quotes] (go (>! c (random-quote))))))
