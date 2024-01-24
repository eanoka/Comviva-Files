import {Role} from "./role.model";
import {AllowDeny} from "../user-management/action.model";

export class AccessiblePermissionsResponse {
    public role: Role
    public cumulative: AllowDeny[]
    public own: AllowDeny[]
}