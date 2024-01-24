import {AllowDeny} from "../user-management/action.model";

export class WorkflowHop {
    public code: string;
    public displayStatus: string;
    public requiredAction: AllowDeny;
}