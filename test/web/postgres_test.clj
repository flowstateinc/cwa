(ns web.postgres-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is]]
   [web.postgres :as sut]
   [web.postgres.employment :as employment]
   [web.postgres.organization :as organization]
   [web.postgres.user :as user]
   [web.postgres.verified-domain :as verified-domain]
   [web.test.system :as test.system]))

(deftest schema
  (test.system/with-system [{:keys [postgres]} (test.system/system)]
    (test.system/truncate postgres)
    (let [execute!     #(sut/execute! postgres %)
          execute-one! #(sut/execute-one! postgres %)

          {user-id ::user/id
           :as     user}
          (execute-one! {:insert-into [:users]
                         :values      [{:email "user@example.com"}]
                         :returning   [:*]})

          {organization-id ::organization/id
           :as             organization}
          (execute-one! {:insert-into [:organizations]
                         :values      [{:name "Organization"}]
                         :returning   [:*]})

          verified-domain
          (execute-one! {:insert-into [:verified-domains]
                         :values      [{:organization-id organization-id
                                        :domain          "example.com"}]
                         :returning   [:*]})

          employment
          (execute-one! {:insert-into [:employments]
                         :values      [{:title           "Employment Title"
                                        :organization-id organization-id
                                        :user-id         user-id}]
                         :returning   [:*]})

          organizations
          (execute! {:select [:o.name :d.domain]
                     :from   [[:organizations :o]]
                     :left-join
                     [[:verified-domains :d] [:= :d.organization-id organization-id]]})

          employees
          (execute! {:select [:e.title :o.name :u.email]
                     :from   [[:employments :e]]
                     :left-join
                     [[:organizations :o] [:= :o.id :e.organization-id]
                      [:users :u] [:= :u.id :e.user-id]]})]

      (doseq [[data spec]
              {employment      (s/keys :req [::employment/created-at
                                             ::employment/id
                                             ::employment/organization-id
                                             ::employment/public-id
                                             ::employment/title
                                             ::employment/updated-at
                                             ::employment/user-id])
               organization    (s/keys :req [::organization/created-at
                                             ::organization/id
                                             ::organization/name
                                             ::organization/public-id
                                             ::organization/updated-at])
               user            (s/keys :req [::user/created-at
                                             ::user/email
                                             ::user/id
                                             ::user/public-id
                                             ::user/updated-at])
               verified-domain (s/keys :req [::verified-domain/created-at
                                             ::verified-domain/domain
                                             ::verified-domain/id
                                             ::verified-domain/updated-at
                                             ::verified-domain/organization-id])}]
        (is (s/valid? spec data)
            (s/explain-str spec data)))

      (is (= [{::organization/name      "Organization"
               ::verified-domain/domain "example.com"}]
             organizations))
      (is (= [{::employment/title  "Employment Title"
               ::organization/name "Organization"
               ::user/email        "user@example.com"}]
             employees)))))
