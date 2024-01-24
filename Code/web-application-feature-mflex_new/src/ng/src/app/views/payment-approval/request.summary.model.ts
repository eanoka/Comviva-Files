import {Client} from "../manage-bill-data/client.model";
import {PaymentRequestHop} from "../payment-approval/payment.request.hop.model";

export class RequestSummary {
    public id: number
    public requester: string
    public requesterEmail: string
    public date: Date
    public payableAmount: number
    public billAmount: number;
    public vat: number;
    public serviceCharge: number;
    public account: Client;
    public attachment : string
    public lastHop : PaymentRequestHop
}