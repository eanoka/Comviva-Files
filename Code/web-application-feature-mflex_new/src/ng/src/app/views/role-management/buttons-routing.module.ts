import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';


import { CreateRoleComponent } from './create-role.component';
import { EditRoleComponent } from './edit-role.component';
import { RolePermsComponent } from './role-perms.component';
import { DeleteRoleComponent } from './delete-role.component';
import { RoleListComponent } from './role-list.component';




const routes: Routes = [
  {
    path: '',
    data: {
      title: 'role-management'
    },
    children: [
      {
        path: '',
        redirectTo: 'role-management'
      },
      {
        path: 'create-role',
        component: CreateRoleComponent,
        data: {
          title: 'Create Role'
        }
      },
      {
        path: 'edit-role',
        component: EditRoleComponent,
        data: {
          title: 'Edit Role'
        }
      },
      {
        path: 'role-perms',
        component: RolePermsComponent,
        data: {
          title: 'Role Perms'
        }
      },
      {
        path: 'delete-role',
        component: DeleteRoleComponent,
        data: {
          title: 'Delete Role'
        }
      },
      {
        path: 'role-list',
        component: RoleListComponent,
        data: {
          title: 'Role List'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ButtonsRoutingModule {}
