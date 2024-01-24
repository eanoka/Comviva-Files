import {Division} from "../manage-bill-data/division.model";
import {Company} from "../manage-bill-data/company.model";
import {RequestSummary} from "../payment-approval/request.summary.model";
import { BillData } from "../manage-bill-data/bill-data.model";
import {PaymentRequestHop} from "../payment-approval/payment.request.hop.model";

class BillRevertibleCache {
    public valuesAsJson: string
}

export class Bill {
    public id: number;
    public accountNo: string;
    public billNo: string;
    public msisdn: number;
    public clientDivision: Division;
    public company: Company;
    public status: string;
    public mfsTxnid: string;
    public billAmount: number
    public serviceCharge: number
    public vat: number
    public dueDate: Date
    public syncDate: Date
    public errorMessage: string
    public billRevertibleCache: BillRevertibleCache
    public billData: BillData
}

export class PaginatedBill {
    public count: number = 0;
    public offset: number = 0;
    public perPage: number = 10;
    public records: Bill[];
    public request: RequestSummary
    public hops: PaymentRequestHop[]
}
