import {Division} from "../manage-bill-data/division.model";
import {Company} from "../manage-bill-data/company.model";
import { User } from "../user-management/user.model";
import { Category } from "../manage-bill-data/category.model";
import { Client } from "../manage-bill-data/client.model";

export class BillDetailTask {
    public id: number;
    public status: string;
    public accountNo: string;
    public clientDivisions: Division[];
    public totalProcessed: number;
    public successCount: number;
    public failedCount: number;
    public startTime: string;
    public endTime: string;
    public category: Category;
    public company: Company;
    public addedBy: User;
    public client: Client;
}

export class PaginatedBillDetailTask {
    public count: number;
    public offset: number;
    public perPage: number;
    public records: BillDetailTask[];
}