import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {NgModule} from '@angular/core';

// Role Management Component
import {BsDropdownModule} from 'ngx-bootstrap/dropdown';
import {CreateRoleComponent} from './create-role.component';
import {EditRoleComponent} from './edit-role.component';
import {RolePermsComponent} from './role-perms.component';
import {DeleteRoleComponent} from './delete-role.component';
import {RoleListComponent} from './role-list.component';

// Buttons Routing
import {ButtonsRoutingModule} from './buttons-routing.module';
import {PaginatorModule} from "../../generic/components/paginator.module";
import {ValidatorModule} from "../../directives/validator.module";

// Angular
@NgModule({
    imports: [
        CommonModule,
        ButtonsRoutingModule,
        BsDropdownModule.forRoot(),
        FormsModule,
        PaginatorModule,
        ValidatorModule
    ],
    declarations: [
        CreateRoleComponent,
        EditRoleComponent,
        RolePermsComponent,
        DeleteRoleComponent,
        RoleListComponent
    ]
})
export class RoleManagementModule {
}