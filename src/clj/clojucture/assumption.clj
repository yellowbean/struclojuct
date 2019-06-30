(ns clojucture.assumption
  (:require [clojucture.type :as t]
            [java-time :as jt]
            [clojure.core.match :as m]
            [clojucture.util :as u])
  (:import [java.time LocalDate]
           [clojucture DoubleFlow]
           [clojucture RateAssumption]))



(defn pick-rate-by-date [ ^LocalDate d index-curve]
  (let [shifted-curve (for [ [ d r ] index-curve] [ (jt/plus d (jt/days -1)) r])]
    (second (last (filter #(jt/after? d (first %)) shifted-curve)))))
  

(defn apply-curve [ curves float-info]
  (let [current-curve ((:index float-info) curves)
        reset-dates (:reset float-info)
        index-rate-at-dates (loop [ d reset-dates r []]
                             (if (nil? d)
                               r
                               (recur (next d)
                                      (conj r (pick-rate-by-date (first d) current-curve)))))]
                                 
        
    (m/match float-info
      { :margin mg}
      (map #(+ mg %) index-rate-at-dates)
      { :factor ft}
      (map #(* ft %) index-rate-at-dates)
      :else nil)))
      
  


(defn setup-curve [ index dates rates]
  (let [ pairs (map vector dates rates)
        p (sort-by first jt/before? pairs)]
    { index p}))

(defn curve-to-df [ n ps]
  (let [ dates (into-array LocalDate (map first ps))
        rs (double-array (map second ps))]
    (DoubleFlow. (name n) dates rs)))

(defn smm2cpr [ ^Double smm]
  (- 1 (Math/pow (- 1 smm) 12)))

(defn cpr2d [ ^Double cpr ]
  (- 1 (Math/pow (- 1 cpr) 1/365)))

(defn d2cpr [ ^Double day-rate]
  (- 1 (Math/pow (- 1 day-rate) 365)))

(defn cpr2smm [ ^Double cpr]
  (- 1 (Math/pow (- 1 cpr) 1/12)))

(defn cdr2smm [ ^Double cdr]
  (cpr2smm cdr))

(defn smm2cdr [ ^Double smm]
  (smm2cpr smm))

(defn gen-pool-assump-df [curve-type v observe-dates]
  (let [ ds (u/dates observe-dates)]
    (as->
      (m/match [curve-type v]
               ;[:smm (v :guard #(vector? %))]
               ;(map #(- 1 (Math/pow (- 1 (second %)) (first %))) factors-m)
               [(:or :cpr :cdr) (v :guard #(vector? %))]
                (map cpr2d v) ; daily rate
               :else nil) rs
      (RateAssumption. (name curve-type) ds (u/ldoubles rs)))))
    
  

(defn gen-asset-assump
  [^RateAssumption pool-assumption observe-dates]
  (let [obs-ary (u/dates observe-dates)]
    (if (= (alength obs-ary) 0)
      nil
      (.apply pool-assumption obs-ary))
    ))



(defn gen-assump-curve [ ds assump ] ; remain dates  assumption
  "convert a pool level assumption to asset level"
  (let [ ppy-curve (:prepayment assump)
        def-curve (:default assump)
        dsa (u/dates ds)
        ;[ recover-curve recovery-lag ] (:recovery assump)
        ]
    ;(println assump)
    ;(println (.project ppy-curve dsa))
    ;(println (.project def-curve dsa))
    {
     :prepayment-curve (.apply ppy-curve dsa)
     :default-curve (.apply def-curve dsa)
     :recovery-curve :nil
     :recover-lag :nil
     }

    ))
