/*
 * Copyright 2012 Pellucid and Zenexity
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datomisca


object SocialNewsSampleData extends SampleData {

  override val schema = Datomic.parseOps("""
    ;; stories
    {:db/id #db/id[:db.part/db]
     :db/ident :story/title
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db/index true
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :story/url
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/unique :db.unique/identity
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :story/slug
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}

    ;; comments
    {:db/id #db/id[:db.part/db]
     :db/ident :comments
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/many
     :db/isComponent true
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :comment/body
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :comment/author
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}

    ;; users
    {:db/id #db/id[:db.part/db]
     :db/ident :user/firstName
     :db/index true
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :user/lastName
     :db/index true
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :user/email
     :db/index true
     :db/unique :db.unique/identity
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :user/upVotes
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/many
     :db.install/_attribute :db.part/db}

    ;; publish time
    {:db/id #db/id[:db.part/db]
     :db/ident :publish/at
     :db/valueType :db.type/instant
     :db/cardinality :db.cardinality/one
     :db/index true
     :db.install/_attribute :db.part/db}

    ;; provenance
    {:db/id #db/id[:db.part/db]
     :db/ident :source/user
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db.install/_attribute :db.part/db}
    {:db/id #db/id[:db.part/db]
     :db/ident :source/confidence
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/doc "Confidence in the source of this information, from 0 to 100"
     :db.install/_attribute :db.part/db}
  """).get

  override val txData = Seq.empty

  val txDatas = Seq(
    Datomic.parseOps("""
      ;; stories
      {:db/id #db/id [:db.part/user]
       :story/title "Teach Yourself Programming in Ten Years"
       :story/url "http://norvig.com/21-days.html"}
      {:db/id #db/id [:db.part/user]
       :story/title "Clojure Rationale"
       :story/url "http://clojure.org/rationale"}
      {:db/id #db/id [:db.part/user]
       :story/title "Beating the Averages"
       :story/url "http://www.paulgraham.com/avg.html"}
    """).get,

    Datomic.parseOps("""
      ;; published stories
      {:db/id #db/id [:db.part/user]
       :story/title "codeq"
       :story/url "http://blog.datomic.com/2012/10/codeq.html"}
      {:db/id #db/id [:db.part/tx]
       :publish/at #inst "2012-11-11"}
    """).get,

    Datomic.parseOps("""
      ;; users
      {:db/id #db/id [:db.part/user]
       :user/firstName "Stu"
       :user/lastName "Halloway"
       :user/email "stuarthalloway@datomic.com"}
      {:db/id #db/id [:db.part/user]
       :user/firstName "Ed"
       :user/lastName "Itor"
       :user/email "editor@example.com"}
    """).get
  )

  val storyWithComments = Datomic.parseOps("""
    {:db/id #db/id[:db.part/user -1]
     :db/ident :storyWithComments
     :story/title "Getting Started"
     :story/url "http://docs.datomic.com/getting-started.html"}
    {:db/id #db/id[:db.part/user -2]
     :comment/body "It woud be great to learn about component attributes."
     :_comments #db/id[:db.part/user -1]}
    {:db/id #db/id[:db.part/user -3]
     :comment/body "I agree."
     :_comments #db/id[:db.part/user -2]}
  """).get
}
