import { CompanyInfo } from "./company-info.model"

export class RequestModal {
    public id: string
    public requestor: string
    public date: Date
    public companies: CompanyInfo[]
}