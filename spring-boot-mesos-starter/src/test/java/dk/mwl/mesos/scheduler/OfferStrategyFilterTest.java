package dk.mwl.mesos.scheduler;

import dk.mwl.mesos.TestHelper;
import org.apache.mesos.Protos;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class OfferStrategyFilterTest {
    Protos.Offer offer = TestHelper.createDummyOffer();

    @Mock
    ResourceRequirement resourceRequirement;

    OfferStrategyFilter filter = new OfferStrategyFilter();
    private String taskId = "taskId";

    @Before
    public void setUp() throws Exception {
        filter.resourceRequirements = new HashMap<>();
        filter.resourceRequirements.put("requirement 1", resourceRequirement);
    }

    @Test
    public void willApproveValidOffer() throws Exception {
        when(resourceRequirement.check("test requirement", taskId, offer)).thenReturn(new OfferEvaluation("test requirement", taskId, offer, true));

        final OfferEvaluation result = filter.evaluate(taskId, offer);
        assertTrue(result.isValid());
        assertSame(offer, result.getOffer());
        verify(resourceRequirement).check("test requirement", taskId, offer);
    }

    @Test
    public void willRejectInvalidOffer() throws Exception {
        when(resourceRequirement.check("test requirement", taskId, offer)).thenReturn(new OfferEvaluation("test requirement", taskId, offer, false));
        assertFalse(filter.evaluate(taskId, offer).isValid());
        verify(resourceRequirement).check("test requirement", taskId, offer);
    }

    @Test @Ignore("A nice to have that's not ready yet")
    public void willNotCheckSecondRequirementIfFirstRejects() throws Exception {
        ResourceRequirement decliningRequirement = mock(ResourceRequirement.class);
        filter.resourceRequirements.put("requirement 2", decliningRequirement);

        when(resourceRequirement.check("test requirement", taskId, offer)).thenReturn(new OfferEvaluation("test requirement", taskId, offer, false));
        when(decliningRequirement.check("test requirement", taskId, offer)).thenReturn(new OfferEvaluation("test requirement", taskId, offer, false));

        assertFalse(filter.evaluate(taskId, offer).isValid());

        verifyZeroInteractions(decliningRequirement);
    }

    @Test
    public void willCheckSecondRequirementIfFirstApproves() throws Exception {
        ResourceRequirement approvingRequirement = mock(ResourceRequirement.class);
        filter.resourceRequirements.put("requirement 2", approvingRequirement);

        when(resourceRequirement.check("test requirement", taskId, offer)).thenReturn(new OfferEvaluation("test requirement", taskId, offer, true));
        when(approvingRequirement.check("test requirement", taskId, offer)).thenReturn(new OfferEvaluation("test requirement", taskId, offer, true));

        assertTrue(filter.evaluate(taskId, offer).isValid());

        verify(approvingRequirement).check("test requirement", taskId, offer);
    }

}