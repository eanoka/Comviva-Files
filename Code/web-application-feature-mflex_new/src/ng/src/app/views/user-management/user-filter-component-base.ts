import {FormHandlerUtil} from "../../generic/form-handler.util";
import {User} from "./user.model";
import {Client} from "../manage-bill-data/client.model";
import {App} from "../../app.model";

class RoleInView {
  public id: number
  public name: string
  public users: User[]
}

export class UserFilterComponentBase extends FormHandlerUtil<User[]> {
  public nullReference: any = null
  public user: User = this.nullReference
  public roleId: number = this.nullReference
  public remarks: string
  public ownerClient: Client = App.user.client
  public op: string = "bar"

  public roles: RoleInView[]
  public users: User[]

  constructor(private _dataFetcherUrl: string, private _dataSubmissionUrl: string, private jsonSubmit: boolean) {
    super(_dataFetcherUrl, _dataSubmissionUrl, jsonSubmit)
  }

  public onChangeRole() {
    this.user = this.nullReference
    this.users = this.roles.findSimilar(this.roleId, function(t) {
      return this.id == t
    }).users
  }

  protected prepareSubmissionData(v: any): any {
    return super.prepareSubmissionData({id: this.user.id, remarks: this.remarks})
  }

  protected onLoadData(x: User[]) {
    let cache = {}
    for(let y of x) {
      let rv: RoleInView = cache[y.role.id] || (cache[y.role.id] = {users: []})
      rv.id = y.role.id
      rv.name = y.role.name
      rv.users.push(y)
    }
    this.roles = Object.values(cache)
  }
} 