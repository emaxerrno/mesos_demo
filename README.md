# Mesos elevator

# Build Instructions

* Must have sbt installed
* simply run ` sbt test `
* It should look like this:
```
[agallego][agallego:~/workspace/mesos_challenge$] (master)
$ sbt test
[info] Loading global plugins from /home/agallego/.sbt/0.13/plugins
[info] Loading project definition from /home/agallego/workspace/mesos_challenge/project
[info] Set current project to elevator (in build file:/home/agallego/workspace/mesos_challenge/)
[info] ElevatorSpec:
[info] - should Increae the size of original list of steps if request is added
[info] - should not Increase size of requests for invalid request
[info] - should not take duplicate requests
[info] - should next strategy should be better than FIFO
[info] - should not crash with no work to do
[info] - should change direction if top floor
[info] Run completed in 663 milliseconds.
[info] Total number of tests run: 6
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 6, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[success] Total time: 1 s, completed Jan 27, 2016 8:59:35 AM
```

# Solution description

## There are 2 main interfaces

* What the user will/can see

```scala
trait ElevatorControlSystem {
  def status(): Map[Elevator,ElevatorState]
  def pickup(src:Floor, dest: Floor)
}
```

* The simulation system

```scala
trait ElevatorSimulation {
  def update(e: Elevator, s: ElevatorRequest): Map[Elevator,ElevatorState]
  def step()
}
```

The main difference here between what is proposed and what the assignment
suggested is that I created an associative container instead of using
a Sequence for simpler/faster/cleaner lookup.

At the moment the only strategy implemented is to take direction into account
first.

```scala
class DirectionFirstElevatorStrategy(val elevators: List[Elevator])
    extends ElevatorControlSystem with ElevatorSimulation {
  private val elevatorLoadDistribution_ = new Random
  private var systemState_ = elevators.map { e =>
    (e -> ElevatorState())
  }.toMap

....

```

Note that FIFO (assignment) is **always** recorded, regardless. This is so that
I could compute the improvement from strategy proposed on the assignment. See
below on **The type system** for notes on the data types.

## The type system

The proposed solution wraps the basic integers into a fully typed system:

```scala
case class Floor(val number: Int)

case class Elevator(val id: Int,
  val max: Floor = Floor(16),
  val min: Floor = Floor(0))

case class ElevatorRequest(
  val from: Floor,
  val dest: Floor)

object ElevatorDirection extends Enumeration {
  val Up, Down = Value // None?
}

case class ElevatorState(
  val original: List[ElevatorRequest] = List(),
  val execOrder: List[ElevatorRequest] = List(),
  val direction: ElevatorDirection.Value = ElevatorDirection.Up,
  val currentFloor: Floor = Floor(0))

```

* One peculiar note here is that the ELevatorState has the original list
of requests which is there to show/compute the improvement from FIFO order

Please see the FIFO ( in ` ElevatorTest.scala ` ) test case showing improvement
of at leats 50%.

## Room for improvement

The proposed improvement does not cover a better distribution among elevators.
This is a low hanging fruit and would only affect one method without changing
any of the rest of the logic. At the moment the load distribution is just random

```scala
  private val elevatorLoadDistribution_ = new Random
```

## Proof of correctness

Though not an idris or coq proof in any formal way, the test cases show a
working system for the specs covered on the website.

There is basic coverage for invalid input (incorrect floor numbers)

```scala
     ...
     ElevatorRequest(Floor(10000), Floor(333333)) // incorrect, above max
     ...
```

There is also coverage floor directionality change (reached top/bottom floor).

There is basic coverage for FIFO improvement.


## Notes on immutability

All of the basic data types are immutable. The only mutable reference is
the pointer holding the system which needs to be swaped when computing
the new state of the system.

This allowed me to think of correctness first above performance.

## Notes on testing

Testing was straight forward, except for the case that we have to record the
current floor on the `step()` function which is different from the instructions.

When you `step()` the state machine you can't siply go into the destination
floor of your next state, you have to first check if you have picked up the
passenger at the requested floor.


## Notes on time

The first basic solution with a single elevator took 3h:20min.

See this commit if you are interested in the basic form:

``` 1e006b8 * Working basic version with unit tests ```

The final solution with pluggable elevators took the full 4hrs allowed.

The readme was written after the coding was done.
