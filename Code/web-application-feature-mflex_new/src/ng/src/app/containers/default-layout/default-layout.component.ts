import {ChangeDetectorRef, Component} from '@angular/core';
import {App} from '../../app.model';
import {NavItemsPopulator} from '../../_nav';
import {SessionUser} from "../../views/user-management/session-user.model";

@Component({
    selector: 'app-dashboard',
    templateUrl: './default-layout.component.html'
})
export class DefaultLayoutComponent {
    public sidebarMinimized = false;
    public navItems = NavItemsPopulator.populate();
    public user = App.user || new SessionUser()

    constructor(private ref: ChangeDetectorRef) {
        if(!this.user.name) {
            App.event.on("_USER_LOADED_", () => {
                this.navItems = NavItemsPopulator.populate();
                this.user = App.user
                this.ref.detectChanges()
            })
        }
    }

    toggleMinimize(e) {
        this.sidebarMinimized = e;
    }
}