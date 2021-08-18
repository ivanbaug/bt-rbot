# Bluetooth robot
It's a bluetooth controlled robot using arduino for a microcontroller and android phone as a remote.

## Mechanical parts
The pieces were laser cutted in 4mm MDF, the files for the cut are in the `parts/` folder.
<img src="images\pieces_01.png" alt="cad" height="250" />
<img src="images\pieces_02.png" alt="pic1" height="250" />
## Electronics
It's a handmade board containing an arduino pro-mini, an hc06 bluetooth module and an lm293d to drive the wheel motors. (Check the `images/` folder for better resolution).

There is also a cheap DC-DC converter from aliexpress to stepdown the battery from 9v to 5v. Arms are driven by two servomotors that feed directly from the 5v source.

<img src="images\electronics_01.png" alt="pic2" height="250" /><img src="images\electronics_02.png" alt="pic3" height="250" />

## Code
There are two parts to this: an arduino file that controls the physical robot.

Then there's an android studio project, made in Java. Beware that this project targets a specific android version (view `app\build.gradle` file) since it was meant to be gifted to a kid in my family that uses a somewhat outdated android phone.

### Video
<a href="https://www.youtube.com/watch?feature=player_embedded&v=EvxlzMIDwuI" target="_blank">
  <img src="https://img.youtube.com/vi/EvxlzMIDwuI/0.jpg"  alt="ROBOT BT" width="240" height="180" border="3" />  
</a>