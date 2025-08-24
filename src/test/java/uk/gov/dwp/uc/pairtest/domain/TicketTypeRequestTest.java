package uk.gov.dwp.uc.pairtest.domain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TicketTypeRequestTest {

    @Test
    void shouldCreateAdultTicketRequestWithCorrectValues() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);
        assertEquals(TicketTypeRequest.Type.ADULT, request.getTicketType());
        assertEquals(3, request.getNoOfTickets());
    }

    @Test
    void shouldCreateChildTicketRequestWithCorrectValues() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        assertEquals(TicketTypeRequest.Type.CHILD, request.getTicketType());
        assertEquals(2, request.getNoOfTickets());
    }

    @Test
    void shouldAllowZeroTickets() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 0);
        assertEquals(0, request.getNoOfTickets());
    }

    @Test
    void shouldHandleNullTicketType() {
        TicketTypeRequest request = new TicketTypeRequest(null, 1);
        assertNull(request.getTicketType());
        assertEquals(1, request.getNoOfTickets());
    }
}
