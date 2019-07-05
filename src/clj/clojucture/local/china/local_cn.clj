(ns clojucture.local.china.local_cn
  (:require
    [clojucture.asset :as asset]
    [clojucture.account :as acc]
    [clojucture.spv :as d]
    [clojucture.bond :as b]
    [clojucture.pool :as p]
    [clojucture.util :as u]
    [clojucture.reader.base :as rb]
    [java-time :as jt]
    [clojure.core.match :as m])
  (:use [clojure.core.match.regex])
  (:import java.util.Arrays)
  )




(defn setup-accounts [d]
  (loop [accs [] accs-to-add (:账户 d)]
    (if (nil? accs-to-add)
      accs
      (recur
        (conj accs
              (m/match (first accs-to-add)
                       {:名称 acc-name} (acc/->account acc-name nil 0 [])
                       :else nil))
        (next accs-to-add)))
    ))
(defn setup-asset-mortgage [a]
  (m/match a
           {:初始面额 original-bal :当前面额 current-bal :年利率 annual-rate :摊销 amort-type :初始日 start-date
            :支付日期 pay-dates :初始期限 original-term :当前期限 remain-term}
           (asset/->mortgage
             {:start-date  (rb/parsing-dates start-date) :balance original-bal :periodicity (jt/months 1) :term original-term
              :period-rate (/ annual-rate 12)}
             nil current-bal (/ annual-rate 12) remain-term nil)


           :else nil
           )

  )

(defn setup-assets [d]
  (let [assets (get-in d [:资产池 :资产清单])
        coll-type (get-in d [:资产池 :类型])]
    (m/match coll-type
             :住房按揭 (map setup-asset-mortgage assets)
             :else nil)
    ))

(defn setup-pool [d]
  (let [assets (setup-assets d)
        cut-off-date (-> (get-in d [:资产池 :封包日]) (rb/parsing-dates))
        ]
    (p/->pool assets cut-off-date)

    ))


(defn setup-dates [d]
  (let [ cut-off-date (jt/local-date (get-in d [:日期 :初始起算日]))
        stated-maturity-date  (get-in d [:日期 :法定到期日])]
    {
     :cut-off-date    cut-off-date
     :stated-maturity stated-maturity-date
     :pay-dates       (rb/parsing-dates (str (get-in d [:日期 :支付日]) "," stated-maturity-date))
     :int-dates       (rb/parsing-dates (str (get-in d [:日期 :计息日]) "," stated-maturity-date))
     :calc-dates      (rb/parsing-dates (str (get-in d [:日期 :计算日]) ","stated-maturity-date))
     :dist-dates      (rb/parsing-dates (str (get-in d [:日期 :信托分配日]) "," stated-maturity-date))
     }
    )
  )

(comment

  (defn setup-triggers [d]

    )

  (defn setup-expense [d]

    )

  (defn setup-bond [d]

    )






  (defrecord china-bank-deal [deal-info opt status update-date]
    d/Deal
    (run-assets [x assump]
      (let [
            ]
        )
      )
    (run-triggers [x]

      )
    (run-bonds [x assump]

      )
    )



  (defn build-deal [deal-type deal-structure]
    (let [
          update (get-in deal-structure [:meta :update-date])
          accounts (setup-accounts deal-structure)
          dates (setup-dates deal-structure)
          pool (setup-pool deal-structure)
          bond (setup-bond deal-structure)
          triggers (setup-triggers deal-structure)
          expense (setup-expense deal-structure)
          ]
      (case deal-type
        ;:bank-deal (china-bank-deal. nil nil nil )

        )
      )
    )
  )