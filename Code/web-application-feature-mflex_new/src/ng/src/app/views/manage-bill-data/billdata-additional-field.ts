import { Bill } from "../payment-request/bill.model";
import { CompanyAdditionalFields } from "./company-additional-fields.model";

export class BilldataAdditionalField
{
	public id:number;
	public value: string;
	public fields: CompanyAdditionalFields;
	public billData: Bill;
}