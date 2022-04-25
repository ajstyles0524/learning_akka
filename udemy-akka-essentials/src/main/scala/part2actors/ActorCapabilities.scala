package part2actors
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorCapabilities.Person.LiveTheLife
object ActorCapabilities extends App{

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi!" =>  println("Replying to "+ sender() + "Hello," + sender() + " How are you ?") // replying to message
      case message: String => println(s"[$self] I have received $message")
      case number: Int => println(s"[simple actor] I have received a Number: $number")
      case SpecialMessage(contents) => println("Received Message: "+ contents +" from -"+ self.path.name)
      case SendMessageToYourself(content) =>
            self ! content
      case SayHiTo(ref) => ref ! "Hi!"
      case WirelessPhoneMessage(content, ref) => ref forward(content + "s")  // i keep the original sender of the wpm
    }
  }
  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "Hello, actor"
  // message can be any type
  simpleActor ! 42
  // in practice use case classes and case objects
  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("some special content")
  // 2- actors have information about their context and about themselves
  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I'm an actor and I'm proud of it")

  // 3- actors can reply to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "Hi!"

  // 5- forwarding message

  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi", bob)

  // Exercises
  /*
  1- a counter actor
     - Increment
     - Decrement
     - Print

  2- a Bank account as an actor receives
      -Deposit an amount
      -Withdraw an amount
      -statement
      -Replies with Success and failure

      interact with some other kind of actor
   */

  // Domain of the Counter
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._

    var count = 0

    override def receive: Receive = {

      case Increment => count += 1
      case Decrement => count -= 1
      case Print =>   println(s"[counter] My current count is $count")
    }
  }

  import Counter._
  val counter = system.actorOf(Props[Counter], "counter")
  (1 to 5 ).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  // Bank account

  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement
    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
  }

  class BankAccount extends Actor {
    import BankAccount._
    var funds = 0

    override def receive: Receive = {

      case Deposit(amount) =>
        if(amount < 0) sender() ! TransactionFailure("invalid deposit amount")
        else{
          funds += amount
          sender() ! TransactionSuccess(s"successfully deposited $amount")
        }
      case Withdraw(amount) =>
         if (amount < 0) sender() ! TransactionFailure("invalid withdraw amount")
         else if (amount > funds) sender() ! TransactionFailure("insufficient funds")
         else{
           funds -= amount
           sender() ! TransactionSuccess(s"successfully withdrew $amount")
         }
      case Statement => sender() ! s"Your balance is $funds"
    }
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }

  class Person extends Actor {
    import Person._
    import BankAccount._

    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(900000)
        account ! Withdraw(5000)
        account ! Statement
      case message => println(message.toString)
    }
  }

  val account = system.actorOf(Props[BankAccount], "bankAccount")
  val person = system.actorOf(Props[Person], "billionaire")
  person ! LiveTheLife(account)

}
