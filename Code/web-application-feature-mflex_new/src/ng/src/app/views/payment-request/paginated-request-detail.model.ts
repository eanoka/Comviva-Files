class RequestDetail {
    public id: number;
    public billCount: number;
    public creationTime: Date;
    public initiator: String;
    public amount: number;
    public status: string;
    public attachment: string;
}

export class PaginatedRequestDetail {
    public count: number;
    public offset: number = 0;
    public perPage: number = 10;
    public records: RequestDetail[];
}