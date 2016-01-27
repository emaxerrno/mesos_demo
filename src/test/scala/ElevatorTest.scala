import org.scalatest._
class ElevatorSpec extends FlatSpec with Matchers {
  val elevator = Elevator(0) // Simpler for testing strategies

  it should "Increae the size of original list of steps if request is added" in
  {
    val strategy = DirectionFirstElevatorStrategy()
    val s1 = strategy.status
    strategy.update(elevator, ElevatorRequest(Floor(2), Floor(3)))
    strategy.status.get(elevator).get.original.size should be (1)
    strategy.update(elevator, ElevatorRequest(Floor(1), Floor(4)))
    strategy.status.get(elevator).get.original.size should be (2)
  }

  it should "not Increase size of requests for invalid request" in
  {
    val strategy = DirectionFirstElevatorStrategy()
    val updates = List(ElevatorRequest(Floor(1), Floor(3)), // correct
      ElevatorRequest(Floor(4), Floor(3)),
      ElevatorRequest(Floor(4), Floor(4)),
      ElevatorRequest(Floor(10000), Floor(333333)) // incorrect, above max
    )
    updates.foreach{ r: ElevatorRequest =>
      strategy.update(elevator,r)
    }
    strategy.status.get(elevator).get.original.size should be(3)
  }

  it should "not take duplicate requests" in
  {
    val strategy = DirectionFirstElevatorStrategy()
    (1 to 3) map { i =>
      strategy.update(elevator,ElevatorRequest(Floor(1), Floor(3)))
    }
    strategy.status.get(elevator).get.original.size should be(3)
    strategy.status.get(elevator).get.execOrder.size should be(1)
  }

  it should "next strategy should be better than FIFO" in
  {
    val strategy = DirectionFirstElevatorStrategy()
    val updates = List(
      ElevatorRequest(Floor(15), Floor(16)),
      ElevatorRequest(Floor(1), Floor(4)),
      ElevatorRequest(Floor(15), Floor(16)),
      ElevatorRequest(Floor(1), Floor(4))
    )
    updates.foreach{ r: ElevatorRequest =>
      strategy.update(elevator,r)
    }

    strategy.status.get(elevator).get.original.size should be(4)
    strategy.status.get(elevator).get.execOrder.size should be(2)

    // execute better than FIFO
    strategy.step() // first need to pick up passengers at source floor 15
    strategy.step()
    strategy.status.get(elevator).get.original.size should be(2)
    strategy.status.get(elevator).get.execOrder.size should be(1)

    // execute better than FIFO
    strategy.step() // first pick up passengers at floor 1
    strategy.step()
    strategy.status.get(elevator).get.original.size should be(0)
    strategy.status.get(elevator).get.execOrder.size should be(0)
  }

  it should "not crash with no work to do" in
  {
    val strategy = DirectionFirstElevatorStrategy()
    (1 to 3) map { i =>
      strategy.step()
    }
    strategy.status.get(elevator).get.original.size should be(0)
    strategy.status.get(elevator).get.execOrder.size should be(0)
  }
  it should "change direction if top floor" in
  {
    val strategy = DirectionFirstElevatorStrategy()
    val updates = List(
      ElevatorRequest(Floor(14), Floor(15)),
      ElevatorRequest(Floor(15), Floor(6))
    )
    updates.foreach{ r: ElevatorRequest =>
      strategy.update(elevator,r)
    }
    var s = strategy.status.get(elevator).get
    // First you need to get to the source floor
    // since we start from 0 to pick up passengers
    strategy.step()
    strategy.step()
    s = strategy.status.get(elevator).get
    s.original.size should be(1)
    s.execOrder.size should be(1)

    strategy.step()
    s = strategy.status.get(elevator).get
    s.original.size should be(0)
    s.execOrder.size should be(0)
  }

}
