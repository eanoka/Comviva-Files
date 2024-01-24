import {Division} from "./division.model";
import {Company} from "./company.model";
import { User } from "../user-management/user.model";
import {BilldataAdditionalField} from "./billdata-additional-field";
import { Bill } from "../payment-request/bill.model";

export class BillData {
    public id: number;
    public accountNo: string;
    public msisdn: number;
    public clientDivision: Division;
    public company: Company;
    public updatedBy: User;
    public modifiedDataFor: BillData;
    public validatedById: number;
    public status: string;
    public billdataAddtionalField: BilldataAdditionalField[]
    public alias: string;
	public bill: Bill;
}

export class PaginatedBillData {
    public count: number;
    public offset: number;
    public perPage: number;
    public records: BillData[];
}
