package com.example.antivirus.license;

/**
 * Ответ клиенту: тикет + цифровая подпись.
 */
public class TicketResponse {

    /**
     * Данные о лицензии.
     */
    private Ticket ticket;

    /**
     * ЭЦП, посчитанная на основе содержимого тикета.
     */
    private String signature;

    public TicketResponse() {
    }

    public TicketResponse(Ticket ticket, String signature) {
        this.ticket = ticket;
        this.signature = signature;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}

