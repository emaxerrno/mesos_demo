import scala.collection.immutable.Map
import scala.util.Random

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

trait ElevatorControlSystem {
  def status(): Map[Elevator,ElevatorState]
  def pickup(src:Floor, dest: Floor)
}
trait ElevatorSimulation {
  def update(e: Elevator, s: ElevatorRequest): Map[Elevator,ElevatorState]
  def step()
}

object DirectionFirstElevatorStrategy {
  def apply() = new DirectionFirstElevatorStrategy(List(Elevator(0)))
  def update(state: ElevatorState, e: Elevator, r: ElevatorRequest): ElevatorState = {
    if (r.from.number >= e.min.number &&
      r.dest.number <= e.max.number) {

      if(state.execOrder.contains(r)){
        state.copy(original= state.original ++ List(r))
      }else{
        val ori = state.original ++ List(r)
        val direction = if (state.currentFloor == e.max){
          ElevatorDirection.Down
        }else if (state.currentFloor == e.min) {
          ElevatorDirection.Up
        }else {
          state.direction
        }
        // split in floors that go in the same direction
        // and floors that go in different direction
        val (nextSteps, tail) = ori.partition { r =>
          direction match {
            case ElevatorDirection.Up =>
              (r.dest.number - r.from.number >= 0 ) && r.from.number >= state.currentFloor.number
            case ElevatorDirection.Down =>
              (r.dest.number - r.from.number <= 0 ) && r.from.number <= state.currentFloor.number
          }
        }

        // println("Next steps:", nextSteps, tail, direction, "current:", state.currentFloor)

        def sortFn(d: ElevatorDirection.Value)(r1: ElevatorRequest, r2: ElevatorRequest): Boolean = {
          d match {
            // missing equality on purpose
            case ElevatorDirection.Up => r1.from.number > r2.from.number
            case ElevatorDirection.Down => r.from.number < r2.from.number
          }
        }
        val (headFn, tailFn) = direction match {
          case ElevatorDirection.Up =>
            (sortFn(ElevatorDirection.Up) _ , sortFn(ElevatorDirection.Down) _)
          case ElevatorDirection.Down =>
            (sortFn(ElevatorDirection.Down) _, sortFn(ElevatorDirection.Up) _ )
        }

        val execution = nextSteps.sortWith{ headFn(_,_)
        } ++ tail.sortWith{ tailFn(_,_)}

        ElevatorState(ori, execution, direction, state.currentFloor)

      }
    }else {
      state
    }
  }
  def step(state: ElevatorState): ElevatorState = {
    val ElevatorState(ori, exec, _, curr) = state
    exec match {
      case head :: tail =>
        if (head.from != state.currentFloor){
          state.copy(currentFloor = head.from)
        }else {
          val dir = if(!tail.isEmpty){
            state.direction match {
              case ElevatorDirection.Up =>
                val th = tail.head // head of tail
                if(th.dest.number >= head.dest.number){
                  ElevatorDirection.Up
                }else {
                  ElevatorDirection.Down
                }
              case ElevatorDirection.Down =>
                val th = tail.head // head of tail
                if(th.dest.number <= head.dest.number){
                  ElevatorDirection.Down
                }else {
                  ElevatorDirection.Up
                }
            }
          }else {
            state.direction
          }
          ElevatorState(ori.filterNot(_ ==  head), tail, dir, head.dest)
        }
      case Nil => state
    }
  }
}

class DirectionFirstElevatorStrategy(val elevators: List[Elevator])
    extends ElevatorControlSystem with ElevatorSimulation {
  private val elevatorLoadDistribution_ = new Random
  private var systemState_ = elevators.map { e =>
    (e -> ElevatorState())
  }.toMap

  override def status() = systemState_
  override def pickup(src:Floor, dest: Floor) = {
    val index = elevatorLoadDistribution_.nextInt(elevators.size)
    val elevator = elevators(index)
    systemState_ = update(elevator, ElevatorRequest(src, dest))
  }
  override def update(e: Elevator, r: ElevatorRequest): Map[Elevator,ElevatorState] = {
    val state =  DirectionFirstElevatorStrategy.update(systemState_(e), e, r)
    systemState_ = systemState_ ++ Map(e->state)
    systemState_
  }
  override def step() = {
    systemState_ = systemState_.map{
      case (elevator,state) =>
        (elevator -> DirectionFirstElevatorStrategy.step(state))
    }.toMap
  }
}



object Elevator extends App {
  Console.println("Please see the unit tests: " + (args mkString ", "))
}
