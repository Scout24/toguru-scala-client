package toguru.impl

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import toguru.api.{Condition, Toggle}
import toguru.implicits.toggle

class ToggleStateSpec extends WordSpec with MustMatchers with MockitoSugar {

  def activation(rollout: Option[Rollout] = None,
                 attrs: Map[String, Seq[String]] = Map.empty) =
    Seq(ToggleActivation(rollout, attrs))

  def rollout(r: Int) = Some(Rollout(r))

  val toggles = List(
    ToggleState("toggle1",
                Map("services" -> "toguru"),
                activation(rollout(30))),
    ToggleState("toggle-2",
                Map.empty[String, String],
                activation(rollout(100))),
    ToggleState("toggle-4",
                Map.empty[String, String],
                activation(
                  attrs = Map("culture" -> Seq("DE", "de-DE"),
                              "version" -> Seq("1", "2")))),
    ToggleState("toggle-5",
                Map.empty[String, String],
                activation(rollout(30),
                           attrs = Map("culture" -> Seq("DE", "de-DE"),
                                       "version" -> Seq("1", "2"))))
  )

  "ToggleState.apply" should {

    "transform activations into conditions" in {
      val condition = toggles(0).condition

      condition mustBe a[UuidDistributionCondition]

      val uuidCondition = condition.asInstanceOf[UuidDistributionCondition]

      uuidCondition.ranges mustBe List(1 to 30)
    }

    "Adds AlwayOffCondition if only attribute contitions are given" in {
      val condition = toggles(2).condition

      condition mustBe All(
        Set(AlwaysOffCondition,
            Attribute("culture", Seq("DE", "de-DE")),
            Attribute("version", Seq("1", "2"))))
    }

    "transform combinations of rollout and attributes to conditions" in {
      val condition = toggles(3).condition

      condition mustBe All(
        Set(
          UuidDistributionCondition(
            List(1 to 30),
            UuidDistributionCondition.defaultUuidToIntProjection),
          Attribute("culture", Seq("DE", "de-DE")),
          Attribute("version", Seq("1", "2"))
        ))
    }
  }

  "ToggleState.activations" should {

    val activations =
      new ToggleStateActivations(ToggleStates(Some(10), toggles))

    "return toggle conditions for services" in {
      val toguruToggles = activations.togglesFor("toguru")

      toguruToggles must have size 1
      toguruToggles.keySet mustBe Set("toggle1")

      val condition = toguruToggles("toggle1")

      condition mustBe a[UuidDistributionCondition]

      val uuidCondition = condition.asInstanceOf[UuidDistributionCondition]

      uuidCondition.ranges mustBe List(1 to 30)
    }

    "return toggle default conditions if toggle is unknown" in {
      val condition = mock[Condition]
      val toggle = Toggle("toggle-3", condition)

      activations.apply(toggle) mustBe condition
    }

    "apply should return togglestates" in {

      activations() mustBe toggles
    }
  }

}
