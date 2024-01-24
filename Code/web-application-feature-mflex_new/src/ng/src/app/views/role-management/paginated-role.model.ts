import {Role} from "./role.model";

export class PaginatedRole {
    public count: number;
    public offset: number;
    public perPage: number;
    public records: Role[];
}