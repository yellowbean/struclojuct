(ns clojucture.bond
  (:require [clojucture.core :as c]
            [java-time :as jt]
            [clojucture.account :as acc]
            [clojucture.util :as u]
            [clojure.core.match :as m])
  (:import
    [tech.tablesaw.api Table DoubleColumn DateColumn]
    [tech.tablesaw.columns AbstractColumn]
    [org.apache.commons.math3.complex Complex]
    [java.time Period LocalDate]
    )
  )


(defprotocol Bond
  (cal-due-principal [ x d ] )
  (cal-due-interest [ x d ] )
  (amortize [ x d amt])
  (cal-next-rate [ x d assump ])
  (receive-payments [ x d principal interest ])
  ;(load [ s ])
  )

(defprotocol Equity
  (cal-max-interest [ x d ])
  )


(defn -amortize [ bond d amt loss ]
  (let [ new-stmt (c/->stmt d :from :principal amt nil) ]
    (->
      (update bond :balance - amt )
      (update :stmts conj new-stmt )
      (assoc :principal-loss loss)
      (assoc-in [:last-payment-date :principal] d)
    )
  ))

(defn -pay-interest [ bond d amt arrears ]
  (let [ new-stmt (c/->stmt d :from :interest amt nil) ]
    (->
      (update bond :stmts conj new-stmt )
      (assoc :interest-arrears arrears)
      (assoc-in [:last-payment-date :int ] d )
      )
    ))

(defrecord sequence-bond
  [ info balance rate stmts last-payment-date interest-arrears principal-loss ]
  Bond
  (cal-due-principal [ x d ]
      (+ balance principal-loss))

  (cal-due-interest [ x d ]
    (u/-cal-due-interest balance (:int last-payment-date) d (info :day-count) rate interest-arrears))
  )

(defrecord equity-bond
  [ info balance stmt last-payment-date ]
  Bond
  (cal-due-principal [ x d ]
    0 )

  (cal-due-interest [ x d ]
    0 )
  Equity
  (cal-max-interest [ x d ]
    (let [ { ul-rate :upper-limit-rate} info
           ul-interest (u/-cal-due-interest balance (:int last-payment-date) d (info :day-count) ul-rate) ]
      ul-interest )
    )
  )

(defrecord schedule-bond
  [ info balance rate stmts last-payment-date interest-arrears principal-loss ]
  Bond
  (cal-due-principal [ x d ]
    (let [ prin-due (u/find-first-in-vec d  (info :amortization-schedule) :dates = :after) ]
      (+ (:principal prin-due) principal-loss)))

  (cal-due-interest [ x d ]
    (u/-cal-due-interest balance (:int last-payment-date) d (info :day-count) rate interest-arrears))
  )


(defn pay-bond-yield [ d acc bond]
  (let [ max-interest (.cal-max-interest bond d)
        acc-after-paid (.try-withdraw acc d (:name bond) max-interest)
        interest-paid (Math/abs (:amount (.last-txn acc-after-paid)))
        ]
    [acc-after-paid (-pay-interest bond d interest-paid 0)]
    )
  )


(defn pay-bond-interest [ d acc bond ]
  (let [ due-int (.cal-due-interest bond d)
         acc-after-paid (.try-withdraw acc d (:name bond) due-int)
         interest-paid (Math/abs (:amount (.last-txn acc-after-paid)))
         interest-arrears (max 0 (- due-int interest-paid)) ]
    [acc-after-paid
     (-pay-interest bond d interest-paid interest-arrears) ]
    )
)

(defn pay-bond-principal [ d acc bond ]
  (let [ due-principal (.cal-due-principal bond d)
         acc-after-paid (.try-withdraw acc d (:name bond) due-principal)
         amortized-principal (Math/abs (:amount (.last-txn acc-after-paid)))
         principal-loss (max 0 (- due-principal amortized-principal)) ]
    [ acc-after-paid
      (-amortize bond d amortized-principal principal-loss) ]
    )
  )

(defn pay-bond-deal [ deal d source bk p ]
  (let [ b (get-in deal [:bond bk])
        a (get-in deal [:account source])]

    (as->
      (case p
      :due-int (pay-bond-interest d a b)
      :due-principal (pay-bond-principal d a b) )
      [update-account update-bond]
          (-> deal
              (assoc-in [:bond bk] update-bond )
              (assoc-in [:account source] update-account) )))


(defn setup-bond [ m ]
  (m/match m
     {:type :sequential
       :info i
       :balance bal :rate r :stmts stmts :last-payment-date last-payment-date :interest-arrears int-arrears :principal-loss prin-loss}
           (->sequence-bond i bal  r stmts last-payment-date int-arrears prin-loss)

      :else nil
     )
  ))