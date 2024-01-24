import {User} from "./user.model";
import {AllowDeny} from "./action.model";

export class AccessiblePermissionsResponse {
    public user: User
    public cumulative: AllowDeny[]
    public own: AllowDeny[]
    public customActions: AllowDeny[]
}