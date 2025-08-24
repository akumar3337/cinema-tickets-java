package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TicketServiceImplTest {

    private TicketPaymentService paymentService;
    private SeatReservationService reservationService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        reservationService = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, reservationService);
    }

    // ✅ Valid purchase: 2 adults
    @Test
    void testValidPurchaseWithAdultsOnly() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        ticketService.purchaseTickets(1L, request);

        verify(paymentService).makePayment(1L, 50);
        verify(reservationService).reserveSeat(1L, 2);
    }

    // ✅ Valid purchase: 1 adult, 1 child, 1 infant
    @Test
    void testValidPurchaseWithMixedTicketsVarArgs() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        ticketService.purchaseTickets(2L, adult, child, infant);

        verify(paymentService).makePayment(2L, 40); // 25 + 15 + 0
        verify(reservationService).reserveSeat(2L, 2); // adult + child
    }

    // ✅ Valid purchase: 1 adult, 1 child, 1 infant
    @Test
    void testValidPurchaseWithMixedTicketsAsArray() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        TicketTypeRequest[] ticketTypeRequests = {adult, child, infant};
        ticketService.purchaseTickets(2L, ticketTypeRequests);

        verify(paymentService).makePayment(2L, 40); // 25 + 15 + 0
        verify(reservationService).reserveSeat(2L, 2); // adult + child
    }

    // ❌ Invalid: accountId is null
    @Test
    void testPurchaseWithNullAccountIdThrowsException() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(null, request)
        );
        assertEquals("Account ID must be a positive non-null value", ex.getMessage());
    }

    // ❌ Invalid: accountId is zero
    @Test
    void testPurchaseWithZeroAccountIdThrowsException() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(0L, request)
        );
        assertEquals("Account ID must be a positive non-null value", ex.getMessage());
    }

    // ❌ Invalid: no ticket requests
    @Test
    void testPurchaseWithNoTicketRequestsThrowsException() {
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L)
        );
    }

    // ❌ Invalid: null ticket request array
    @Test
    void testPurchaseWithNullTicketRequestArrayThrowsException() {
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null)
        );
        assertEquals("At least one ticket request is required", ex.getMessage());
    }

    // ❌ Invalid: one null ticket request in array
    @Test
    void testPurchaseWithNullTicketRequestThrowsException() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, request, null)
        );
        assertEquals("Invalid ticket request: null or non-positive ticket count", ex.getMessage());
    }

    // ❌ Invalid: ticket request with null type
    @Test
    void testPurchaseWithNullTicketTypeThrowsException() {
        TicketTypeRequest request = new TicketTypeRequest(null, 1);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, request)
        );
        assertEquals("Invalid ticket request: null or non-positive ticket count", ex.getMessage());
    }

    // ❌ Invalid: ticket request with zero quantity
    @Test
    void testPurchaseWithZeroTicketQuantityThrowsException() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, request)
        );
        assertEquals("Invalid ticket request: null or non-positive ticket count", ex.getMessage());
    }

    // ❌ Invalid: ticket request with negative quantity
    @Test
    void testPurchaseWithNegativeTicketQuantityThrowsException() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -1);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, request)
        );
        assertEquals("Invalid ticket request: null or non-positive ticket count", ex.getMessage());
    }

    // ❌ Invalid: child ticket without adult
    @Test
    void testPurchaseWithChildOnlyThrowsException() {
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, child)
        );
        assertEquals("Child or infant tickets require at least one adult ticket", ex.getMessage());
    }

    // ❌ Invalid: infant ticket without adult
    @Test
    void testPurchaseWithInfantOnlyThrowsException() {
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, infant)
        );
        assertEquals("Child or infant tickets require at least one adult ticket", ex.getMessage());
    }

    // ❌ Invalid: more infants than adults
    @Test
    void testPurchaseWithMoreInfantsThanAdultsThrowsException() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, adult, infant)
        );
        assertEquals("Each infant must be accompanied by one adult", ex.getMessage());
    }

    // ❌ Invalid: more than 25 tickets
    @Test
    void testPurchaseWithTooManyTicketsThrowsException() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, adult)
        );
        assertEquals("Cannot purchase more than 25 tickets", ex.getMessage());
    }

    // ❌ Invalid: more than 25 tickets
    @Test
    void testPurchaseWithTooManyTicketsThrowsExceptionWithInfant() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 6);
        InvalidPurchaseException ex = assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, adult, infant)
        );
        assertEquals("Cannot purchase more than 25 tickets", ex.getMessage());
    }

    // ✅ Valid: exactly 25 tickets
    @Test
    void testPurchaseWithMaxAllowedTickets() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25);
        ticketService.purchaseTickets(1L, adult);

        verify(paymentService).makePayment(1L, 625);
        verify(reservationService).reserveSeat(1L, 25);
    }

    // ✅ Valid: exactly 25 tickets
    @Test
    void testPurchaseWithMaxAllowedTicketsMixed() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 15);
        TicketTypeRequest children = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 5);
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 5);

        ticketService.purchaseTickets(1L, adult, children, infant);

        verify(paymentService).makePayment(1L, 450);
        verify(reservationService).reserveSeat(1L, 20);
    }

    // ✅ Valid: infant ticket with matching adult
    @Test
    void testPurchaseWithEqualInfantsAndAdults() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        ticketService.purchaseTickets(1L, adult, infant);

        verify(paymentService).makePayment(1L, 50);
        verify(reservationService).reserveSeat(1L, 2);
    }
}
