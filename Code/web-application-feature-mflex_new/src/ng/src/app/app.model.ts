import $ from 'jquery'
import {SessionUser} from "./views/user-management/session-user.model";

export class App {
    private static config: any

    public static event: any = $({})
    public static user: SessionUser
    public static basePath: string = location.href.substring(0, location.href.length - location.hash.length) + 'api';
    public static getConfig(key: string): string {
        return App.config[key]
    }
    public static setConfig(_config: any) {
        App.config = _config;
    }
}