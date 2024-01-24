import {User} from "./user.model";

export class PaginatedUser {
    public count: number;
    public offset: number;
    public perPage: number;
    public records: User[];
}