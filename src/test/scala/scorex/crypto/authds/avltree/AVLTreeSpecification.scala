package scorex.crypto.authds.avltree

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.PropSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scorex.crypto.authds.TwoPartyTests

import scala.util.Success

class AVLTreeSpecification extends PropSpec with GeneratorDrivenPropertyChecks with TwoPartyTests {

  val KL = 26
  val VL = 8

  property("AVLModifyProof serialization") {

    val wt = new AVLTree(KL)
    var digest = wt.rootHash()
    forAll(kvGen) { case (aKey, aValue) =>
      whenever(aKey.length == KL && aValue.length == VL) {
        digest shouldEqual wt.rootHash()
        val proof = wt.modify(aKey, replaceLong(aValue))
        digest = proof.verify(digest, replaceLong(aValue)).get
        val parsed = AVLModifyProof.parseBytes(proof.bytes)(KL, 32).get

        parsed.key shouldEqual proof.key
        parsed.proofSeq.indices foreach { i =>
          parsed.proofSeq(i).bytes shouldEqual proof.proofSeq(i).bytes
        }
        parsed.bytes shouldEqual proof.bytes
      }
    }
  }

  property("AVLTree stream") {
    val wt = new AVLTree(KL)
    var digest = wt.rootHash()
    forAll(kvGen) { case (aKey, aValue) =>
      digest shouldEqual wt.rootHash()
      val proof = wt.modify(aKey, replaceLong(aValue))
      digest = proof.verify(digest, replaceLong(aValue)).get
    }
  }

  property("AVLTree insert") {
    val wt = new AVLTree(KL)
    forAll(kvGen) { case (aKey, aValue) =>
      val digest = wt.rootHash()
      val proof = wt.modify(aKey, rewrite(aValue))
      proof.verify(digest, rewrite(aValue)).get shouldEqual wt.rootHash()
    }
  }


  property("AVLTree update") {
    val wt = new AVLTree(KL)
    forAll(genBoundedBytes(KL, KL), genBoundedBytes(VL, VL), genBoundedBytes(VL, VL)) {
      (key: Array[Byte], value: Array[Byte], value2: Array[Byte]) =>
        whenever(!(value sameElements value2)) {
          val digest1 = wt.rootHash()
          val proof = wt.modify(key, replaceLong(value))
          proof.verify(digest1, replaceLong(value)).get shouldEqual wt.rootHash()

          val digest2 = wt.rootHash()
          val updateProof = wt.modify(key, replaceLong(value2))
          updateProof.verify(digest2, replaceLong(value2)).get shouldEqual wt.rootHash()
        }
    }
  }


  def rewrite(value: AVLValue): UpdateFunction = {
    oldOpt: Option[AVLValue] => Success(value)
  }

  def kvGen: Gen[(Array[Byte], Array[Byte])] = for {
    key <- Gen.listOfN(KL, Arbitrary.arbitrary[Byte]).map(_.toArray)
    value <- Gen.listOfN(VL, Arbitrary.arbitrary[Byte]).map(_.toArray)
  } yield (key, value)


}
