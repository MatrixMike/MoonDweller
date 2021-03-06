(ns moon-dweller.state
  (:require clojure.walk
            [moon-dweller.util :as u]
            [dommy.core :as dom :refer-macros [sel sel1]]))

(declare print-message)

(def total-weight 12)
(def ignore-words '(that is the
                    fucking fuckin
                    damn please)) 
(def current-room 0)             ; Current room the player is in.
(def visited-rooms [])           ; Rooms that the player has visited.
(def inventory [])               ; Players inventory of items.
(def credits 0)                  ; Players credits (aka $$$).
(def milestones #{})             ; Players milestones. Used to track and manipulate story.
(def messages [])                ; Message queue.
(def alive true)                 ; Alive or not?
(def game-options {:retro true   ; Print to stdout with tiny pauses between characters.
                   :sound true}) ; Play sound during gameplay.

(defn text-speed []
  (if (game-options :retro) 12 1))

; A vector containing the objects that each room contains when the game starts. Each index
; corresponds to the room as defined in 'rooms'.
(def room-objects
  (vector
    [0 1]        ;0
    []           ;1
    [2]          ;2
    []           ;3
    []           ;4
    []           ;5
    []           ;6
    [7]          ;7
    []           ;8
    []           ;9
    [8]          ;10
    [9 10]       ;11
    [11]         ;12
    []           ;13
    []           ;14
    [12 13 14]   ;15
    [18]         ;16
    [15 16 17]   ;17
    []           ;18
    [20]         ;19
    []           ;20
    [21 22 26]   ;21
    []           ;22
    [23]         ;23
    []           ;24
    []           ;25
    [27]         ;26
    [25]         ;27
    [24]         ;28
    []           ;29
    []           ;30
    [29]         ;31
    []))         ;32

; Specifies the verbs that users can identify an object with (a gun might
; be "gun", "weapon", etc). A set means that the given term may refer to
; multiple objects. The system will try to deduce the correct object when
; a command is entered. Each index corresponds to the same index in room-objects.
(def object-identifiers
    {'candy 0 'bar 0 'bed 1 'lever 2 'mag 3 'magazine 3 'porno 3 'porn 3 'nudey 3 'boy 7
     'alien 7 'teenager 7 'keycard #{4 5 6} 'key #{4 5 6} 'man #{8 9 21 22 23} 'robot 10
     'green #{4 13} 'red #{5 12} 'brown 14 'silver 6 'bum 11 'potion #{12 13 14}
     'credits 18 'attendant 15 'woman 15 'rum 16 'whisky 17 'lagavulin 17
     'web 20 'knife 19 'small #{19 29} 'thin 22 'skinny 22 'fat 21 'paper #{24 29}
     'book #{25 30} 'bent 30 'artificial 30 'stone 26 'rock 26 'floorboard 27
     'floorboards 27 'floor 27 'staircase 28 'stairs 28})

(defn md-pr [text i]
  (set! messages (conj messages [text i])))

(defn consume-messages []
  (if (> (count messages) 0)
    (do
      (apply print-message (first messages))
      (set! messages (apply vector (rest messages))))
    (.setTimeout js/window consume-messages 200)))

(defn print-message [text i]
  "Prints a string one character at a time with an interval of i milliseconds"
  (let [li (dom/create-element :li)]
    (letfn [(populate [t]
             (if (not (empty? t))
               (let [f (first t)
                     html (dom/html li)]
                 (dom/set-html! li (str html f))
                 (.setTimeout js/window #(populate (rest t)) i))
               (do
                 (u/enable-input!)
                 (u/scroll-to-bottom)
                 (consume-messages))))]
      (u/disable-input!)
      (populate text)
      (dom/append! (sel1 :#history) li)
      (u/scroll-to-bottom))))

(defn print-with-newlines
  ([lines speed] (print-with-newlines lines speed ""))
  ([lines speed prepend]
    "Prints a sequence of strings in list format."
    (if (not (empty? prepend))
      (md-pr prepend speed))
    (doseq [l lines]
      (md-pr (str " - " l) speed))))

(defn set-option! [option value]
  "Sets one of the pre-defined game options. Assumes valid input."
  (set! game-options (assoc game-options option value)))

(defn valid-option? [option]
  (let [opts (keys game-options)]
    (boolean (some #{option} opts))))

(defn can-afford? [n]
  (>= credits n))

(defn hit-milestone? [m]
  (some #(= % m) milestones))

(defn add-milestone! [m]
  "Adds the given milestone to the players list"
  (set! milestones (conj milestones m)))

(defn remove-milestone! [m]
  "Removes the given milestone from the players list"
  (set! milestones (vec (remove #(= m %) milestones))))

(defn set-current-room! [room]
    (set! current-room room))

(defn kill! []
  (set! alive false))

(defn alive? []
  alive)

(defn objects-in-room ([] (objects-in-room current-room))
  ([room]
   (nth room-objects room)))

(defn room-has-object?
  "Returns true if the gien room currently houses the given object"
  ([objnum] (room-has-object? current-room objnum))
  ([room objnum]
   (boolean (some #{objnum} (objects-in-room room)))))

(defn in-inventory? [objnum]
  "Returns true if object assigned to 'objnum' is in players inventory"
  (boolean (some #{objnum} inventory)))

(letfn
  [(alter-room! [room changed]
     "Physically alters the contents of the given"
     (set! room-objects 
           (assoc-in room-objects [room] changed)))]

  (defn take-object-from-room!
    ([objnum] (take-object-from-room! current-room objnum))
    ([room objnum]
     (alter-room! room
                  (vec (remove #(= objnum %)
                                 (objects-in-room room))))))

  (defn drop-object-in-room!
    ([objnum] (drop-object-in-room! current-room objnum))
    ([room objnum]
     (alter-room! room
                  (conj (objects-in-room room) objnum)))))

(defn remove-object-from-inventory! [objnum]
  "Physically removes an object from the players inventory."
  (set! inventory (vec (remove #(= % objnum) inventory))))
 
(defn add-object-to-inventory! [objnum]
  "Physically adds an object to the players inventory."
  (set! inventory (conj inventory objnum)))

(defn pay-the-man! [c]
  (set! credits (+ credits c)))

(defn visit-room! [room]
  (set! visited-rooms (conj visited-rooms room)))

(defn save-game! []
  (let [game-state {:current-room current-room
                    :inventory inventory
                    :visited-rooms visited-rooms
                    :credits credits
                    :milestones milestones
                    :game-options game-options
                    :room-objects room-objects}
        ls (aget js/window "localStorage")]
    (.setItem ls "md_state" (.stringify js/JSON
                                        (clj->js game-state)))))
(defn load-game! []
  (let [ls (aget js/window "localStorage")
        md-state (.parse js/JSON 
                         (.getItem ls "md_state"))]
    (if (nil? md-state)
      false
      (let [game-state (js->clj md-state)]
        (set! current-room (game-state "current-room"))
        (set! inventory (game-state "inventory"))
        (set! visited-rooms (game-state "visited-rooms"))
        (set! credits (game-state "credits"))
        (set! milestones (map keyword (game-state "milestones")))
        (set! game-options (clojure.walk/keywordize-keys (game-state "game-options")))
        (set! room-objects (u/extend-vec (game-state "room-objects") room-objects))
        true))))
