(ns clogo.executor-test
  (:require [clojure.test :refer :all]
            [clogo.executor :refer :all]))

;; Test commands for a turtle agent which just remembers the commands it receives
(defn test-cmd-a [ttl] (str ttl " A"))
(defn test-cmd-b [ttl x] (str ttl " B(" x ")"))
(def test-cmd-c ^{:step 3} (fn [ttl x] (str ttl " C(" x ")")))

;; Returns an executor agent bound to a test turtle
(defn test-ag []
  (let [test-ttl (agent "")]
    (executor test-ttl {:a test-cmd-a :b test-cmd-b :c test-cmd-c})))

(deftest exec-or-queue-cmd-should-execute-a-command-if-turtle-is-ready
  (testing "exec-or-queue-cmd should execute a command if executor is ready"
    (let [exec-ag  (test-ag)
          test-ttl (:turtle @exec-ag)]
      (exec-or-queue-cmd @exec-ag exec-ag test-cmd-a [])
      (await test-ttl)
      (is (= " A" @test-ttl))
      (exec-or-queue-cmd @exec-ag exec-ag test-cmd-b [1])
      (await test-ttl)
      (is (= " A B(1)" @test-ttl))
      (is (empty? (:cmd-queue @exec-ag))))))

(deftest exec-or-queue-cmd-should-queue-a-command-if-turtle-is-busy
  (testing "exec-or-queue-cmd should queue a command if executor is busy"
    (let [exec-ag (test-ag)]
      (send exec-ag #(assoc % :state :busy))
      (send exec-ag (fn [ex] (exec-or-queue-cmd ex exec-ag test-cmd-b [22])))
      (await exec-ag)
      (is (= [[test-cmd-b [22]]] (:cmd-queue @exec-ag)))
      (send exec-ag (fn [ex] (exec-or-queue-cmd ex exec-ag test-cmd-a [])))
      (await exec-ag)
      (is (= [[test-cmd-b [22]] [test-cmd-a []]] (:cmd-queue @exec-ag))))))

(deftest end-cmd-should-revert-to-ready-state-if-cmd-queue-is-empty
  (testing "end-cmd should revert to ready state if command queue is empty"
    (let [exec-ag (test-ag)]
      (send exec-ag #(assoc % :state :busy :cmd-queue []))
      (await exec-ag)
      (is (= :ready (:state (end-cmd @exec-ag exec-ag)))))))

(deftest end-cmd-should-execute-queued-cmd-if-cmd-queue-is-not-empty
  (testing "end-cmd should executed queued command if command queue is not empty"
    (let [exec-ag (test-ag)
          test-ttl (:turtle @exec-ag)]
      (send exec-ag #(assoc % :state :busy :cmd-queue [[test-cmd-a []]]))
      (await exec-ag)
      (is (= "" @test-ttl))
      (send-end-cmd exec-ag)
      (await exec-ag)
      (await test-ttl)
      (is (= " A" @test-ttl))
      (is (= :ready (:state @exec-ag)))
      (is (= [] (:cmd-queue @exec-ag)))
      )))

