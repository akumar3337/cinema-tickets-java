package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
    private static final int MAX_TICKETS_PER_PURCHASE = 25;
    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;
    private static final int INFANT_TICKET_PRICE = 0;

    private final TicketPaymentService paymentService;
    private final SeatReservationService reservationService;

    public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
        this.paymentService = paymentService;
        this.reservationService = reservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateAccount(accountId);
        validateTicketRequests(ticketTypeRequests);

        int totalSeats = calculateTotalSeats(ticketTypeRequests);
        if(totalSeats>0){
            int totalAmount = calculateTotalAmount(ticketTypeRequests);
            if(totalAmount>0){
                paymentService.makePayment(accountId, totalAmount);
            }
            reservationService.reserveSeat(accountId, totalSeats);
        }

    }

    private void validateAccount(Long accountId) throws InvalidPurchaseException {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account ID must be a positive non-null value");
        }
    }

    private void validateTicketRequests(TicketTypeRequest... ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket request is required");
        }

        int totalTickets = 0;
        int adultTickets = 0;
        int childTickets = 0;
        int infantTickets = 0;

        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {

            validateTicketTypeRequest(ticketTypeRequest);
            totalTickets += ticketTypeRequest.getNoOfTickets();

            switch (ticketTypeRequest.getTicketType()) {
                case ADULT:
                    adultTickets += ticketTypeRequest.getNoOfTickets();
                    break;
                case CHILD:
                    childTickets += ticketTypeRequest.getNoOfTickets();
                    break;
                case INFANT:
                    infantTickets += ticketTypeRequest.getNoOfTickets();
                    break;
                default:
                    throw new InvalidPurchaseException("Unknown ticket type");
            }
        }

        validateMaxLimitPerPurchase(totalTickets);
        validateAdultTicketRequirement(adultTickets, childTickets, infantTickets);
        validateInfantToAdultRatio(adultTickets, infantTickets);
    }

    private void validateInfantToAdultRatio(int adultTickets, int infantTickets) {
        if (infantTickets > adultTickets) {
            throw new InvalidPurchaseException("Each infant must be accompanied by one adult");
        }
    }

    private void validateAdultTicketRequirement(int adultTickets, int childTickets, int infantTickets) {
        if ((childTickets > 0 || infantTickets > 0) && adultTickets == 0) {
            throw new InvalidPurchaseException("Child or infant tickets require at least one adult ticket");
        }
    }

    private void validateMaxLimitPerPurchase(int totalTickets) {
        if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException("Cannot purchase more than " + MAX_TICKETS_PER_PURCHASE + " tickets");
        }
    }

    private void validateTicketTypeRequest(TicketTypeRequest ticketTypeRequest) {
        if (ticketTypeRequest == null || ticketTypeRequest.getTicketType() == null || ticketTypeRequest.getNoOfTickets() <= 0) {
            throw new InvalidPurchaseException("Invalid ticket request: null or non-positive ticket count");
        }
    }

    private int calculateTotalAmount(TicketTypeRequest... ticketTypeRequests) {
        int totalAmount = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            switch (request.getTicketType()) {
                case ADULT:
                    totalAmount += request.getNoOfTickets() * ADULT_TICKET_PRICE;
                    break;
                case CHILD:
                    totalAmount += request.getNoOfTickets() * CHILD_TICKET_PRICE;
                    break;
                case INFANT:
                    totalAmount += request.getNoOfTickets() * INFANT_TICKET_PRICE;
                    break;
            }
        }

        return totalAmount;
    }

    private int calculateTotalSeats(TicketTypeRequest... ticketTypeRequests) {
        int totalSeats = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {
            // Infants don't get seats, they sit on adult's lap
            if (request.getTicketType() != TicketTypeRequest.Type.INFANT) {
                totalSeats += request.getNoOfTickets();
            }
        }

        return totalSeats;
    }

}
