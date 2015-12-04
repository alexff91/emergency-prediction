package ru.spbstu.dis.kb;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class KnowledgeBaseTest {
  @Test
  public void shouldInferInSimpleKnowledgeBase_case1()
  throws Exception {
    // given
    final SimpleKnowledgeBase simpleKnowledgeBase = new SimpleKnowledgeBase();

    // when
    final ChosenAction chosenAction = simpleKnowledgeBase
        .inferOutput(new DataInput(5, 1));

    // then
    assertThat(chosenAction).isEqualTo(new ChosenAction("Action2"));
  }

  @Test
  public void shouldInferInSimpleKnowledgeBase_case2()
  throws Exception {
    // given
    final SimpleKnowledgeBase simpleKnowledgeBase = new SimpleKnowledgeBase();

    // when
    final ChosenAction chosenAction = simpleKnowledgeBase
        .inferOutput(new DataInput(1235, 1));

    // then
    assertThat(chosenAction).isEqualTo(new ChosenAction("Action1"));
  }
}

class SimpleKnowledgeBase implements KnowledgeBase {
  @Override
  public ChosenAction inferOutput(final DataInput input) {
    if (input.getPressure() > 10) {
      return new ChosenAction("Action1");
    } else {
      return new ChosenAction("Action2");
    }
  }
}